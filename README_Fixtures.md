
# Account Test Fixtures

This project contains two main test fixtures used to simulate and test different scenarios for **Checking** and **Savings** accounts. The tests are written using **JUnit** and are executed using **Gradle**. Each fixture takes a series of scenarios in a specific format and simulates account behavior over time.

## Table of Contents
- [Running Tests](#running-tests)
- [CheckingAccountTestFixture](#checkingaccounttestfixture)
- [SavingsAccountTestFixture](#savingsaccounttestfixture)
- [Untested Scenarios](#untested-scenarios)

## Running Tests

To run the tests, use the following Gradle commands depending on which fixture you want to execute:

### CheckingAccountTestFixture:
- **Run with string scenario input:**
    ```
    gradle runCheckingFixture --args="-s '100, 10|20, , 10, true, 80'"
    ```

- **Run with file input:**
    ```
    gradle runCheckingFixture --args="-f src/test/resources/CheckingAccountTest.csv"
    ```

### SavingsAccountTestFixture:
- **Run with string scenario input:**
    ```
    gradle runSavingsFixture --args="-s '0, 300, 25, 0.02, 5|5, 100|200, 5, 166.8'"
    ```

- **Run with file input:**
    ```
    gradle runSavingsFixture --args="-f src/test/resources/SavingsAccountTest.csv"
    ```

## CheckingAccountTestFixture

The `CheckingAccountTestFixture` takes test scenarios in the following format:
```
"initial balance, checks, withdrawals, deposits, number of months to simulate, expected balance"
```
For example:
```
"100, 10|20, , 10, true, 80"
```
This simulates a checking account with an initial balance of $100, where two checks of $10 and $20 are issued, and a deposit of $10 is made over the course of 80 months.

## SavingsAccountTestFixture

The `SavingsAccountTestFixture` takes test scenarios in the following format:
```
"initial Balance, minimum Balance, minimum balance Fee, interest, withdrawals, deposits, number of months to simulate, expected balance"
```
For example:
```
"0, 300, 25, 0.02, 5|5, 100|200, 5, 166.8"
```
This simulates a savings account starting with an initial balance of $0, a minimum balance of $300, a minimum balance fee of $25, an interest rate of 0.02, and withdrawals and deposits happening over 5 months.

## Untested Scenarios

The following scenarios are not covered by the current fixtures:

1. **Negative Interest Rates**:
   - Negative interest rates throw an error and are not tested.

2. **Empty Savings Account with Minimum Balance Fee**:
   - For savings accounts, if the balance is $0, the minimum balance fee is applied. This may result in a negative balance, but these cases are not handled explicitly in the tests.

3. **Negative Balances with Non-Zero Interest**:
   - If a balance goes negative, and the interest rate is non-zero, this situation is not managed by the current test logic.

4. **Complex Deposit/Withdrawal Timing**:
   - The code assumes all deposits and withdrawals are applied in month zero. Scenarios where transactions occur in later months, especially with non-zero interest, are not handled.

5. **Order of Operations (Withdrawals/Deposits)**:
   - The current logic doesn't consider the order of operations. For example, withdrawing $100 from an empty account followed by depositing $100 results in a final balance of $0. However, the actual sequence of events isn't simulated accurately.

## Special Scenarios for CheckingAccountTestFixture

Here are some additional edge cases that may break the current logic:

1. **Multiple Checks with Insufficient Funds**:
   - Test what happens when multiple checks are written, and the account doesn't have enough funds to cover them all.

2. **Negative Withdrawals or Deposits**:
   - Introducing negative values for withdrawals or deposits may cause undefined behavior or errors.

3. **Extremely High Number of Transactions**:
   - Handling an extremely high number of transactions in one month may expose performance issues.

4. **Overdraft Followed by Large Deposit**:
   - Overdrawing the account followed by a large deposit may reveal issues with how negative balances are handled.

5. **Initial Balance as Null or Invalid**:
   - Using a non-numeric value or an empty string as the initial balance may cause the test to break.

