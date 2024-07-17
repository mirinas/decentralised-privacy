import json
import re
import csv
import sys

from tabulate import tabulate

import requests

s = requests.Session()
s.headers.update({'Authorization': 'Bearer'})

channel = 'ch1'
contract = 'privacy'
password = 'adminpw'
port = None


def main():

    print('Client started')
    while True:
        line = input()
        args = re.split('\\s+', line)
        cmd = args.pop(0)

        if 'quit' == cmd:
            print("Stopping client")
            return

        elif 'import' == cmd:
            if len(args) < 1:
                print('No file specified')
                continue

            file = args.pop(0)
            with open(file) as cqs:
                rows = csv.reader(cqs, dialect='excel')
                for row in rows:
                    success = select_user(row.pop(0), 'owners.org')
                    if not success:
                        break
                    for cq in row:
                        if cq == '':
                            continue
                        chaincode('invoke', 'SetConstraint', [cq])
            print('... Done')

        elif '@' in cmd:
            select_user(cmd.split('@')[0], cmd.split('@')[1])

        elif not port:
            print('Enroll user first')
            continue

        elif 'register' == cmd:
            if len(args) < 1:
                print('No count specified')
                continue

            count = int(args.pop())
            name = 'user'
            if len(args) > 0:
                name = args.pop(0)

            for i in range(1, count + 1):
                req('/user/register', {
                    'id': name + str(i),
                    'secret': password
                })
            print('... Done')

        elif 'query' == cmd:
            res = chaincode('query', 'QueryTable', args)
            print_table(res, args.pop())

        elif 'Upload' == cmd:
            if len(args) < 2:
                print('No key or data specified')
                continue

            transient = args.pop()
            res = chaincode('invoke', 'Upload', args, transient)
            print(res)

        else:
            res = chaincode('invoke', cmd, args)
            print(res)


def print_table(data, atom):
    name = re.search('[A-Za-z]+', atom)
    head = re.search('(?<=\\()(.*?)(?=\\))', atom)
    if not name or not head:
        print('Invalid table:\n' + atom + ': ' + data)
        return

    head = head.group(0).split(',')
    name = name.group(0)
    head.insert(0, name)

    rows = map(lambda row: row.split(','), data.split('\n'))
    print(tabulate(rows, headers=head, tablefmt='presto'))


def chaincode(operation, func, args, transient=None):
    data = {
        'method': func,
        'args': args
    }
    if transient:
        data['transient'] = {'data': transient}

    res = req('/' + operation + '/' + channel + '/' + contract, data)

    if 'response' not in res:
        return str(res)
    return res['response']


def select_user(name, domain):
    ports = {
        'owners.org': 8800,
        'org1.co': 8801,
        'org2.ac': 8802,
        'org3.gov': 8803
    }
    global port
    port = ports[domain]

    res = req('/user/enroll', {
        'id': name,
        'secret': password
    })
    if 'token' not in res:
        return False

    s.headers.update({'Authorization': 'Bearer ' + res['token']})
    print(res['token'])
    return True


def req(endpoint, data):
    res = s.post(
        'http://localhost:' + str(port) + endpoint,
        data=json.dumps(data)
    )
    return res.json()


if __name__ == '__main__':
    main()
