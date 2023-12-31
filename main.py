import difflib
import json
import os
import re
import subprocess
import sys

src_path = ".\\src"


def clr():
    os.system(R"echo off")
    os.system(R"del /Q tr_tmp\*.* ")
    os.system(R"del /S /Q tr_tmp\classes\* ")
    os.system(R"echo on")


def compile_jar():
    current_address = src_path
    src_files = []
    for parent, dirnames, filenames in os.walk(current_address):
        for filename in filenames:
            if re.match(R".*\.java", filename):
                src_files.append(os.path.join(parent, filename))

    # print(src_files)
    os.system(f"javac -d .\\tr_tmp\\classes -encoding utf-8 -sourcepath  tr_tmp\\classes {' '.join(src_files)}")
    os.system(f"jar -cfm .\\tr_tmp\\compiler.jar .\\MANIFEST.MF -C .\\tr_tmp\\classes .")


test_path = [
    (R'testsuit2022\full\my',1),
    (R'testsuit2022\full\A',30), 
    (R'testsuit2022\full\B',30), 
    (R'testsuit2022\full\C',30),
    (R'testsuit2022\full\shj',93)
    ]
#test_path = [(R'testsuit2022\full\C',30)]
#test_path = [(R'testsuit2022\full\shj',93)]
#test_path = [(R'testsuit2022\full\my',1)]
mars_path = R'C:\Users\11067\.jdks\corretto-15.0.2\bin\java.exe -jar ..\Mars.jar mc Default me nc .\tr_tmp\mips.txt'


def run():
    statistic_file_path = R".\performance.json"
    statistic = None
    if os.path.exists(statistic_file_path):
        with open(statistic_file_path, "r", encoding='utf-8') as f:
            statistic = json.load(f)
    else:
        statistic = {}
    
    for (base_path,num) in test_path:
        base_path = os.path.abspath(base_path)
        
        if not statistic.get(os.path.split(base_path)[1]):
            statistic[os.path.split(base_path)[1]] = {}
        for i in range(1, num+1):
            file_path = os.path.join(base_path, f'testfile{i}.txt')

            if not os.path.exists(file_path):
                print(f"{file_path} not found.\n")
                continue
            in_path = os.path.join(base_path, f'input{i}.txt')
            out_path = os.path.join(base_path, f'output{i}.txt')
            print(f'exec {file_path}...\n')
            #mips_name = f'{os.path.split(base_path)[1]}_mips{i}.txt'
            mips_name = 'mips.txt'
            os.system(f'java -jar .\\tr_tmp\\compiler.jar {file_path} tr_tmp\\{mips_name}')
            if not os.path.exists(R".\tr_tmp\mips.txt"):
                print(f'no asm gen in {file_path}\n')
                return
            with open(in_path, 'r', encoding='utf-8') as fin, open('tr_tmp\\ans.txt', 'w', encoding='utf-8') as fout:

                r = subprocess.run(mars_path, input=fin.read().replace(' ','\n'), stdout=fout, text=True)
                if r.returncode != 0:
                    print(f'err exec MARS in {file_path}.\n')
                    return
            d = difflib.Differ()
            with open('tr_tmp\\ans.txt', 'r', encoding='utf-8') as f_me, open(out_path, 'r', encoding='utf-8') as f_std:
 
                me = f_me.read().strip()
                stdd = f_std.read().strip()
                if me != stdd:
                    r = list(d.compare(me.splitlines(True), stdd.splitlines(True)))
                
                    print(f'wa in {file_path}.\n')
                    print(''.join(r))
                    return
            if not statistic[os.path.split(base_path)[1]].get(f'testfile{i}'):
                statistic[os.path.split(base_path)[1]][f'testfile{i}'] = []
            with open("InstructionStatistics.txt","r", encoding='utf-8') as f:
                t = list(map(lambda s:s.strip(),f.read().split(':')))
                #print(t)
                statistic[os.path.split(base_path)[1]][f'testfile{i}'].append(t[6])
            #os.system(R"del tr_tmp\mips.txt")
    
    with open(statistic_file_path, "w", encoding='utf-8') as f:
            json.dump(statistic, f)

clr()
compile_jar()
run()

R"java -jar -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005 .\tr_tmp\compiler.jar testsuit2022\full\shj\testfile5.txt  tr_tmp\mips.txt"