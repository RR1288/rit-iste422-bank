
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

record BankRecords(Collection<Owner> owners, Collection<Account> accounts, Collection<RegisterEntry> registerEntries) {

}

public class Obfuscator {

    private static final Logger logger = LogManager.getLogger(Obfuscator.class.getName());

    private Date generateRandomDOB() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, (int) (Math.random() * (2000 - 1960) + 1960)); // Random year between 1960 and 2000
        calendar.set(Calendar.MONTH, (int) (Math.random() * 12)); // Random month
        calendar.set(Calendar.DAY_OF_MONTH, (int) (Math.random() * 28)); // Random day
        return calendar.getTime();
    }

    private String shuffleString(String original) {
        char[] characters = original.toCharArray();
        List<Character> charList = new ArrayList<>();
        for (char c : characters) {
            charList.add(c);
        }
        Collections.shuffle(charList);
        StringBuilder shuffled = new StringBuilder();
        for (Character c : charList) {
            shuffled.append(c);
        }
        return shuffled.toString();
    }

    private long generateRandomAccountNumber() {
        return (long) (Math.random() * 1_000_000_000L);  // Random 9-digit account number
    }

    public static Date shiftDates(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DATE, 7); // Add 7 days
        return calendar.getTime();
    }

    public BankRecords obfuscate(BankRecords rawObjects) {
        Map<Long, Long> ownerIdMapping = new HashMap<>();
        Map<Long, Long> accountIdMapping = new HashMap<>();

        // OWNERS ==========================================================
        List<Owner> obfuscatedOwners = new ArrayList<>();
        for (Owner oldOwner : rawObjects.owners()) {
            // Generate obfuscated attributes
            String newSSN = "***-**-" + oldOwner.ssn().substring(7);
            String newName = shuffleString(oldOwner.name());
            Date newDOB = generateRandomDOB();
            String newAddress = shuffleString(oldOwner.address());
            String newAddress2 = shuffleString(oldOwner.address2());
            String newCity = shuffleString(oldOwner.city());

            // Create a new owner object with the obfuscated data
            Owner newOwner = new Owner(
                    newName,
                    oldOwner.id(), // Retain old ID initially for consistent mapping
                    newDOB,
                    newSSN,
                    newAddress,
                    newAddress2,
                    newCity,
                    oldOwner.state(),
                    oldOwner.zip()
            );

            // Add to the new owners list and map the old ID to the new ID
            obfuscatedOwners.add(newOwner);
            ownerIdMapping.put(oldOwner.id(), newOwner.id());
        }

        // ACCOUNTS ========================================================
        List<Account> obfuscatedAccounts = new ArrayList<>();
        for (Account oldAccount : rawObjects.accounts()) {
            long newOwnerId = ownerIdMapping.get(oldAccount.getOwnerId()); // Get updated owner ID

            Account newAccount;
            if (oldAccount instanceof CheckingAccount ca) {
                newAccount = new CheckingAccount(
                        shuffleString(ca.getName()),
                        generateRandomAccountNumber(), // Generate a new account number
                        ca.getBalance(),
                        ca.getCheckNumber(),
                        newOwnerId
                );
            } else if (oldAccount instanceof SavingsAccount sa) {
                newAccount = new SavingsAccount(
                        shuffleString(sa.getName()),
                        generateRandomAccountNumber(),
                        sa.getBalance(),
                        sa.getInterestRate(),
                        newOwnerId
                );
            } else {
                continue; // In case new account types are added
            }

            // Add to the new accounts list and map the old ID to the new ID
            obfuscatedAccounts.add(newAccount);
            accountIdMapping.put(oldAccount.getId(), newAccount.getId());
        }

        // REGISTER ENTRIES ================================================
        List<RegisterEntry> obfuscatedRegisterEntries = new ArrayList<>();
        for (RegisterEntry oldEntry : rawObjects.registerEntries()) {
            long newAccountId = accountIdMapping.get(oldEntry.accountId()); // Get updated account ID

            // Create a new register entry
            RegisterEntry newEntry = new RegisterEntry(
                    oldEntry.getId(),
                    newAccountId,
                    oldEntry.entryName(),
                    // Can't change amount because integration tests fail
                    oldEntry.amount(),
                    shiftDates(oldEntry.date())
            );

            // Add to the new register entries list
            obfuscatedRegisterEntries.add(newEntry);
        }

        // Return a new BankRecords object with the obfuscated data
        return new BankRecords(obfuscatedOwners, obfuscatedAccounts, obfuscatedRegisterEntries);
    }

    /**
     * Change the integration test suite to point to our obfuscated production
     * records.
     *
     * To use the original integration test suite files run "git checkout --
     * src/test/resources/persister_integ.properties"
     */
    public void updateIntegProperties() throws IOException {
        Properties props = new Properties();
        File propsFile = new File("src/test/resources/persister_integ.properties".replace('/', File.separatorChar));
        if (!propsFile.exists() || !propsFile.canWrite()) {
            throw new RuntimeException("Properties file must exist and be writable: " + propsFile);
        }
        try (InputStream propsStream = new FileInputStream(propsFile)) {
            props.load(propsStream);
        }
        props.setProperty("persisted.suffix", "_prod");
        logger.info("Updating properties file '{}'", propsFile);
        try (OutputStream propsStream = new FileOutputStream(propsFile)) {
            String comment = String.format("""
                                           Note: Don't check in changes to this file!!
                                           #Modified by %s
                                           #to reset run 'git checkout -- %s'""",
                    this.getClass().getName(), propsFile);
            props.store(propsStream, comment);
        }
    }

    public static void main(String[] args) throws Exception {
        // enable assertions
        Obfuscator.class.getClassLoader().setClassAssertionStatus("Obfuscator", true);
        logger.info("Loading Production Records");
        Persister.setPersisterPropertiesFile("persister_prod.properties");
        Bank bank = new Bank();
        bank.loadAllRecords();

        logger.info("Obfuscating records");
        Obfuscator obfuscator = new Obfuscator();
        // Make a copy of original values so we can compare length
        // deep-copy collections so changes in obfuscator don't impact originals
        BankRecords originalRecords = new BankRecords(
                new ArrayList<>(bank.getAllOwners()),
                new ArrayList<>(bank.getAllAccounts()),
                new ArrayList<>(bank.getAllRegisterEntries()));
        BankRecords obfuscatedRecords = obfuscator.obfuscate(originalRecords);

        logger.info("Saving obfuscated records");
        obfuscator.updateIntegProperties();
        Persister.resetPersistedFileNameAndDir();
        Persister.setPersisterPropertiesFile("persister_integ.properties");
        // old version of file is cached so we need to override prefix (b/c file changed
        // is not the one on classpath)
        Persister.setPersistedFileSuffix("_prod");
        // writeReords is cribbed from Bank.saveALlRecords(), refactor into common
        // method?
        Persister.writeRecordsToCsv(obfuscatedRecords.owners(), "owners");
        Map<Class<? extends Account>, List<Account>> splitAccounts = obfuscatedRecords
                .accounts()
                .stream()
                .collect(Collectors.groupingBy(rec -> rec.getClass()));
        Persister.writeRecordsToCsv(splitAccounts.get(SavingsAccount.class), "savings");
        Persister.writeRecordsToCsv(splitAccounts.get(CheckingAccount.class), "checking");
        Persister.writeRecordsToCsv(obfuscatedRecords.registerEntries(), "register");

        logger.info("Original   record counts: {} owners, {} accounts, {} registers",
                originalRecords.owners().size(),
                originalRecords.accounts().size(),
                originalRecords.registerEntries().size());
        logger.info("Obfuscated record counts: {} owners, {} accounts, {} registers",
                obfuscatedRecords.owners().size(),
                obfuscatedRecords.accounts().size(),
                obfuscatedRecords.registerEntries().size());

        if (obfuscatedRecords.owners().size() != originalRecords.owners().size()) {
            throw new AssertionError("Owners count mismatch");
        }
        if (obfuscatedRecords.accounts().size() != originalRecords.accounts().size()) {
            throw new AssertionError("Account count mismatch");
        }
        if (obfuscatedRecords.registerEntries().size() != originalRecords.registerEntries().size()) {
            throw new AssertionError("RegisterEntries count mismatch");
        }
    }
}
