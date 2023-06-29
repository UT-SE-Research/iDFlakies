import json
import os
import sys

def main(args):
    minimized_file = args[1]    # File to parse for info
    polluters = args[2]         # Polluters that should be detected (separated by |)
    cleaners = args[3]          # Cleaners that should be detected (separated by |)

    # Parse json output
    with open(minimized_file) as f:
        data = json.load(f)

    # Match expected polluters with the found polluters
    actual_polluters = set()
    for p in data['polluters']:
        actual_polluters |= set(p['deps'])
    expected_polluters = set(polluters.split('|'))
    if not actual_polluters == expected_polluters:  # If does not match, exit with non-zero code
        exit(1)

    # Match expected cleaners with the found cleaners
    actual_cleaners = set()
    for p in data['polluters']:
        if 'cleanerData' in p:
            for c in p['cleanerData']['cleaners']:
                actual_cleaners |= set(c['cleanerTests'])
    expected_cleaners = set(cleaners.split('|'))
    if not actual_cleaners == expected_cleaners:    # If does not match, exit with non-zero code
        exit(1)

if __name__ == '__main__':
    main(sys.argv)
