
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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

record BankRecords(Collection<Owner> owners, Collection<Account> accounts, Collection<RegisterEntry> registerEntries) {

}

public class Obfuscator {

    private static Logger logger = LogManager.getLogger(Obfuscator.class.getName());
    private ObfuscatorUtils obsUtils = new ObfuscatorUtils();

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

    private long getNewOwnerIdForAccount(long oldOwnerId, Collection<Owner> obfuscatedOwners) {
        // Find the new obfuscated owner ID corresponding to the old one
        for (Owner o : obfuscatedOwners) {
            if (o.getId() == oldOwnerId) {
                return o.getId(); // Return new obfuscated ownerId
            }
        }
        throw new IllegalArgumentException("Owner ID not found");
    }

    private long getNewAccountIdForRegisterEntry(long oldAccountId, Collection<Account> obfuscatedAccounts) {
        for (Account a : obfuscatedAccounts) {
            if (a.getId() == oldAccountId) {
                return a.getId(); // Return new obfuscated accountId
            }
        }
        throw new IllegalArgumentException("Account ID not found");
    }

    public BankRecords obfuscate(BankRecords rawObjects) {

        // OWNERS ==========================================================
        List<Owner> newOwners = new ArrayList<>();
        for (Owner o : rawObjects.owners()) {
            // Mask SSN
            String new_ssn = "***-**-" + o.ssn().substring(7);

            // Substitute name
            //String new_name = "Obfuscated Name";

            // Substitute dob
            Date new_dob = generateRandomDOB();

            // Shuffle address
            String new_address = shuffleString(o.address());
            String new_address2 = shuffleString(o.address2());

            // Shuffle city
            String new_city = shuffleString(o.city());

            newOwners.add(new Owner(o.name(), o.id(), new_dob, new_ssn, new_address, new_address2, new_city, o.state(), o.zip()));
        }

        Collection<Owner> obfuscatedOwners = newOwners;

        // ACCOUNTS ==========================================================
        List<Account> newAccounts = new ArrayList<>();

        for (Account a : rawObjects.accounts()) {
            // Replace the ownerId with a new random ID or a corresponding obfuscated ID
            long newOwnerId = getNewOwnerIdForAccount(a.getOwnerId(), obfuscatedOwners);
            Account newAccount;
            if (a instanceof CheckingAccount) {
                CheckingAccount ca = (CheckingAccount) a;
                newAccount = new CheckingAccount(ca.getName(), a.getId(), ca.getBalance(), ca.getCheckNumber(), newOwnerId);
            } else if (a instanceof SavingsAccount) {
                SavingsAccount sa = (SavingsAccount) a;
                newAccount = new SavingsAccount(sa.getName(), a.getId(), sa.getBalance(), sa.getInterestRate(), newOwnerId);
            } else {
                continue; // Handle other account types as needed
            }
            newAccounts.add(newAccount);
        }
        Collection<Account> obfuscatedAccounts = newAccounts;

        // ENTRIES ==========================================================
        List<RegisterEntry> newRegisterEntries = new ArrayList<>();
        for (RegisterEntry re : rawObjects.registerEntries()) {
            long newAccountId = getNewAccountIdForRegisterEntry(re.accountId(), obfuscatedAccounts);
            newRegisterEntries.add(new RegisterEntry(re.getId(), newAccountId, re.entryName(), re.amount(), re.date()));
        }
        Collection<RegisterEntry> obfuscatedRegisterEntries = newRegisterEntries;

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
            String comment = String.format(
                    "Note: Don't check in changes to this file!!\n"
                    + "#Modified by %s\n"
                    + "#to reset run 'git checkout -- %s'",
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
