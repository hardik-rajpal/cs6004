import os,sys
if(len(sys.argv)<2):
    print('give tcdir')
    exit(0)
tcdir = sys.argv[1]
tcfilenames = os.listdir(tcdir)
for tcfilename in tcfilenames:
    if( not tcfilename.endswith('.java')):
        continue
    tcbasename = tcfilename.split('.java')[0]
    tcfilepath = os.path.join(tcdir,tcfilename)
    jimpleGenCmd = "sootup "+tcdir+" -f J "+tcbasename
    print(jimpleGenCmd)
    finalCmd ="""
              powershell;
              function sootup { java -cp soot.jar soot.Main -pp -cp $args }
              """ + jimpleGenCmd + ";"+ """
              
              """
    (os.system(finalCmd))
    break