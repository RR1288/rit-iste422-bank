import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/* TODO: Add these lines to build.gradle to add the runSavingsFixture target:
task runSavingsFixture(type: JavaExec) {
    group = "Execution"
    description = "Run SavingsAccountTestFixture class"
    classpath = sourceSets.test.runtimeClasspath
    mainClass = "SavingsAccountTestFixture"
}
 */

public class SavingsAccountTestFixture {
    public static Logger logger = LogManager.getLogger(SavingsAccountTestFixture.class);
    // Note that we could also load the file from the classpath instead of hardcoding the pathname
    static final String TEST_FILE = "src/test/resources/SavingsAccountTest.csv".replace('/', File.separatorChar);

    record TestScenario(double initBalance,
                        double minBalance,
                        double minFee,
                        double interestRate,
                        List<Double> withdrawals,
                        List<Double> deposits,
                        int runMonthEndNTimes,
                        double endBalance
    ) { }

    private static List<TestScenario> testScenarios;

    @Test
    public void runTestScenarios() throws Exception {
        if (testScenarios == null) {
            System.err.println("\n\n");
            System.err.println("************************************");
            System.err.println("************************************");
            System.err.println();
            System.err.println("Note: NOT running any Test Scenarios");
            System.err.println("Run main() method to run scenarios!!");
            System.err.println();
            System.err.println("************************************");
            System.err.println("************************************");
            System.err.println("\n\n");
            return;
        }

        // iterate over all test scenarios
        for (int testNum = 0; testNum < testScenarios.size(); testNum++) {
            TestScenario scenario = testScenarios.get(testNum);
            logger.info("**** Running test for {}", scenario);

            // set up account with specified starting balance and interest rate
            // TODO: Add code to create account....
            //String name, long id, double balance, double interestRate, long ownerId
            SavingsAccount sa = new SavingsAccount(
                "test "+testNum, -1, scenario.initBalance, scenario.interestRate, -1);
            
            sa.setMinimumBalance(scenario.minBalance);
            sa.setBelowMinimumFee(scenario.minFee);
            // now process withdrawals, deposits
            // TODO: Add code to process withdrawals....
            for (double withdrawalAmount : scenario.withdrawals) {
                sa.withdraw(withdrawalAmount);
            }
            // TODO: Add code to process deposits
            for (double depositAmount : scenario.deposits) {
                sa.deposit(depositAmount);
            }

            // run month-end if desired and output register
            if (scenario.runMonthEndNTimes > 0) {
                // TODO: Add code to run month-end....
                for (RegisterEntry entry : sa.getRegisterEntries()) {
                    logger.info("Register Entry {} -- {}: {}", entry.id(), entry.entryName(), entry.amount());

                }

                for (int i = 0; i < scenario.runMonthEndNTimes; i++){
                    sa.monthEnd();
                }
            }

            // make sure the balance is correct
            // TODO: add code to verify balance
            assertThat("Test #" + testNum + ":" + scenario, Math.round(sa.getBalance()*100d)/100d, is(scenario.endBalance));
        }
    }

    private static void runJunitTests() {
        JUnitCore jc = new JUnitCore();
        jc.addListener(new TextListener(System.out));
        Result r = jc.run(SavingsAccountTestFixture.class);
        System.out.printf("Tests run: %d Passed: %d Failed: %d\n",
                r.getRunCount(), r.getRunCount() - r.getFailureCount(), r.getFailureCount());
        System.out.println("Failures:");
        for (Failure f : r.getFailures()) {
            System.out.println("\t"+f);
        }
    }

    // NOTE: this could be added to TestScenario class
    private static List<Double> parseListOfAmounts(String amounts) {
        if (amounts.trim().isEmpty()) {
            return List.of();
        }
        List<Double> ret = new ArrayList<>();
        logger.debug("Amounts to split: {}", amounts);
        for (String amtStr : amounts.trim().split("\\|")) {
            logger.debug("An Amount: {}", amtStr);
            ret.add(Double.parseDouble(amtStr));
        }
        return ret;
    }

    // NOTE: this could be added to TestScenario class
    private static TestScenario parseScenarioString(String scenarioAsString) {
        String [] scenarioValues = scenarioAsString.split(",");
        System.out.println(scenarioValues);
        // should probably validate length here
        double initialBalance = Double.parseDouble(scenarioValues[0]);
        // Minimum balance
        double minBalance = Double.parseDouble(scenarioValues[1]);
        // Minimum balance fee
        double minFee = Double.parseDouble(scenarioValues[2]);
        // TODO: parse the rest of your fields
        double interest = Double.parseDouble(scenarioValues[3]);
        List<Double> wds = parseListOfAmounts(scenarioValues[4]);
        List<Double> deposits = parseListOfAmounts(scenarioValues[5]);
        int nTimes = Integer.parseInt(scenarioValues[6].strip());
        double endBalance =  Double.parseDouble(scenarioValues[7]);
        
        // TODO: Replace these dummy values with _your_ field values to populate TestScenario object
        TestScenario scenario = new TestScenario(
                initialBalance, minBalance, minFee, interest, wds, deposits, nTimes, endBalance
        );
        return scenario;
    }

    private static List<TestScenario> parseScenarioStrings(List<String> scenarioStrings) {
        logger.info("Parsing test scenarios...");
        List<TestScenario> scenarios = new ArrayList<>();
        for (String scenarioAsString : scenarioStrings) {
            if (scenarioAsString.trim().isEmpty()) {
                continue;
            }
            TestScenario scenario = parseScenarioString(scenarioAsString);
            scenarios.add(scenario);
        }
        return scenarios;
    }

    public static void main(String [] args) throws IOException {
        System.out.println("START TESTING");

        // TODO: Instead of hardcoded "false", determine if tests are coming from file or cmdline
        // Note: testsFromFile is just a suggestion, you don't have to use testsFromFile or even an if/then statement!
        System.out.println("Command-line arguments passed in: " + java.util.Arrays.asList(args));
        boolean testsFromFile = false;
        boolean testsFromString = false;

        if (args.length != 0 && args.length % 2 == 0) {
            if (args[0].equals("-f")) {
                testsFromFile = true;
            }
            if (args[0].equals("-s")) {
                testsFromString = true;
            }
        }

        // Note: this is just a suggestion, you don't have to use testsFromFile or even an if/then statement!
        if (testsFromFile) {
            // if populating with scenarios from a CSV file...
            // TODO: We could get the filename from the cmdline, e.g. "-f CheckingAccountScenarios.csv"
            System.out.println("\n\n****** FROM FILE (cmd)******\n");
            // TODO: get filename from cmdline and use instead of TEST_FILE constant
            List<String> scenarioStringsFromFile = Files
                .readAllLines(Paths.get(args[1].replace('/', File.separatorChar)));
            // Note: toArray converts from a List to an array
            testScenarios = parseScenarioStrings(scenarioStringsFromFile);
            runJunitTests();
        }
        
        if (testsFromString){
            System.out.println("\n\n****** FROM String (cmd) ******\n");
            // if specifying a scenario on the command line,
            // for example "-t '10, 20|20, , 40|10, 0'"
            // Note the single-quotes above ^^^ because of the embedded spaces and the pipe symbol
            List<TestScenario> parsedScenarios = parseScenarioStrings(Collections.singletonList(args[1]));
            testScenarios = parsedScenarios;

            // TODO: write the code to "parse" scenario into a suitable string
            // TODO: get TestScenario object from above string and store to testScenarios static var
            runJunitTests();
        }
        System.out.println("DONE");
    }
}
