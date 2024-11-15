import random

def sample_lines_with_seed(input_file, output_file, num_samples, seed_value):
    with open(input_file, 'r') as file:
        lines = file.readlines()
    
    random.seed(seed_value)
    sampled_lines = random.sample(lines, num_samples)
	
    with open(output_file, 'w') as file:
        file.writelines(sampled_lines)

input_file = './filename.txt'  # Replace with your input file name
output_file = 'sampled_contracts.txt'  # Replace with your desired output file name
num_samples = 10  # Number of lines to sample
seed_value = 1  # Set your desired seed value

# Call the function
sample_lines_with_seed(input_file, output_file, num_samples, seed_value)