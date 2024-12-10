
Obfuscation Methods
-------------------

1. **Randomized Date of Birth**
   - Generates a random date of birth (DOB) between 1960 and 2000 for obfuscating owner records.
   - Why: Protects sensitive personal information while maintaining a realistic range.

2. **String Shuffling**
   - Randomizes the characters in sensitive strings like:
     - Owner's name
     - Address fields (address, address2, city)
     - Account's name
   - Why: Obscures the original values while retaining length.

3. **Masked Social Security Numbers (SSN)**
   - Partially masks SSNs, replacing the first five digits with `***-**` while retaining the last four digits.
   - Why: Ensures privacy while preserving format for validation purposes.

4. **Randomized Account Numbers**
   - Generates new random 9-digit account numbers for each account.
   - Why: Prevents traceability of account information.

5. **Date Shifting**
   - Adjusts register entry dates by adding 7 days to the original date.
   - Why: Obscures specific transaction timing while retaining chronological order.