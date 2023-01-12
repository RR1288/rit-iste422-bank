import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class CheckingAccount extends Account {
    public static Logger logger = LogManager.getLogger(CheckingAccount.class.getName());

    private long checkNumber;

    public CheckingAccount() {
        logger.debug("Creating no-arg checking account");
        checkNumber = 0;
    }

    public CheckingAccount(String name, long id, double balance, long checkNumber, long ownerId) {
        super(name, id, balance, ownerId);
        logger.debug(String.format("Creating checking account for %d: %s, %f",
               ownerId, name, balance));
        this.checkNumber = checkNumber;
    }

    /**
     * write a check
     *
     * @param name
     * @param amount
     * @return check number
     * @throws Exception if negative balance
     */
    public long writeCheck(String name, double amount) throws Exception {
    	logger.debug("Balance before check:" + getBalance() + " check amount: " + amount);
        withdraw(amount, String.format("Check %d", checkNumber));
    	logger.debug("Balance after check:" + getBalance());

        return checkNumber++;
    }

    @Override
    public void monthEnd() {
        if (getBalance() < getMinimumBalance()) {
            withdraw(getBelowMinimumFee(), "MINIMUM BALANCE CHARGE");
        }
        logger.info("Check # at end of month: " + checkNumber);
        register.add("END CHECK", (double)checkNumber);
    }

    public String toString() {
        return "Checking Account " + super.toString() + " Current Check #" + checkNumber;
    }

    public static CheckingAccount fromCSV(String csv) throws SerializationException {
        final String [] fields = csv.split(",");
        final String version = fields[fields.length-1].trim();
        if (! version.equals("v1")) {
            throw new SerializationException("Verison incorrect or missing, expected v1 but was " + version);
        }
        if (fields.length != 6) {
            throw new SerializationException("not enough fields, should be 6 but were " + fields.length + ": " + csv);
        }
        return new CheckingAccount(
                // Fields in object: String name, long id, double balance, long checkNumber, long ownerId
                fields[1].trim(),
                Long.parseLong(fields[0].trim()),
                Double.parseDouble(fields[2].trim()),
                Long.parseLong(fields[3].trim()),
                Long.parseLong(fields[4].trim())
        );
    }

    public String toCSV() {
        // Fields: String name, long id, double balance, long checkNumber, long ownerId
        return String.format("%d, %s, %f, %d, %d, v1",
                getId(),
                name,
                getBalance(),
                checkNumber,
                getOwnerId()
        );
    }


}
