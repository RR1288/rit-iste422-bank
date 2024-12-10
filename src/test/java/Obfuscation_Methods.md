
Obfuscation Methods
-------------------

1. **Randomized Date of Birth**
   - Generates a random date of birth (DOB) between 1960 and 2000 for obfuscating owner records.

2. **String Shuffling**
   - Randomizes the characters in sensitive strings like:
     - Owner's name
     - Address fields (address, address2, city)

3. **Masked Social Security Numbers (SSN)**
   - Partially masks SSNs, replacing the first five digits with `***-**` while retaining the last four digits.

4. **Randomized Account Numbers**
   - Generates new random 9-digit account numbers for each account.

5. **Date Shifting**
   - Adjusts register entry dates by adding 7 days to the original date.
