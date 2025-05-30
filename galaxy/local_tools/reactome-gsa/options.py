import csv

def get_distinct_values(dataset, column_name):
    options = []
    print("Got here")
    if dataset is None:
        return options
    if column_name is None:
        return options
    with open(dataset.file_name, newline="") as handle:
        reader = csv.DictReader(handle, skipinitialspace=True)
        if column_name not in reader.fieldnames:
            return []
        for row in reader:
            options.append((row[column_name], row[column_name], False))
    options.append("test", "test", False)
    return options