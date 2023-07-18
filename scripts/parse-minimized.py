import json
import os
import sys

def main(args):
    minimized_file = args[1]    # File to parse for info

    expected_polluters = set(args[2].split('|'))    # Polluters that should be detected (separated by |)
    if args[3] == '':                               # Cleaners that should be detected (separated by |), if there are any
        expected_cleaners = set()
    else:
        expected_cleaners = set(args[3].split('|'))

    # Parse json output
    with open(minimized_file) as f:
        data = json.load(f)

    # Match expected polluters with the found polluters
    actual_polluters = set()
    for p in data['polluters']:
        actual_polluters |= set(p['deps'])
    if not actual_polluters == expected_polluters:  # If does not match, exit with non-zero code
        print('EXPECTED POLLUTERS:', sorted(expected_polluters))
        print('ACTUAL POLLUTERS:', sorted(actual_polluters))
        exit(1)

    # Match expected cleaners with the found cleaners
    actual_cleaners = set()
    for p in data['polluters']:
        if 'cleanerData' in p:
            for c in p['cleanerData']['cleaners']:
                actual_cleaners |= set(c['cleanerTests'])
    if not actual_cleaners == expected_cleaners:    # If does not match, exit with non-zero code
        print('EXPECTED CLEANERS:', sorted(expected_cleaners))
        print('ACTUAL CLEANERS:', sorted(actual_cleaners))
        exit(2)

if __name__ == '__main__':
    main(sys.argv)
