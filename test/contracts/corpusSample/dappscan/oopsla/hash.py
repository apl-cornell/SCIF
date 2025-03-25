# Run in https://github.com/InPlusLab/DAppSCAN/tree/9ee36460a4468e70436033fe75d964cce4ab3a6d

import glob
import json
import hashlib
import csv
import os

pattern = os.path.join("DAppSCAN-source", "**", "*.json")
json_files = glob.glob(pattern, recursive=True)

results = []

for file in json_files:
    with open(file, "r") as f:
        content = f.read()
        if "SWC-107-Reentrancy" not in content:
            continue

        data = json.loads(content)
        file_path = data.get("filePath")
        if not file_path:
            continue

        # Deterministic SHA-256 hash of the "filePath" field from JSON
        hash_value = hashlib.sha256(file_path.encode("utf-8")).hexdigest()
        results.append((file_path, hash_value))

# Sort by hash value
results.sort(key=lambda x: x[1])

# Write the output to a CSV file
file = "reentrancy.csv"
with open(file, "w", newline="") as csvfile:
    writer = csv.writer(csvfile)
    writer.writerow(["filePath", "hash (64 character hex)"])
    writer.writerows(results)

print(f"CSV file {file} has been created with the file path hashes sorted in ascending order.")
