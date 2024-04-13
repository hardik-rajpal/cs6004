cd 200050048-pa2/
rm -rf *.class */*.class testcase sootOutput

javac -cp .:../soot.jar PA2.java

passed=0
total=0

for folder in ../testcases/*
do
    mkdir testcase
    cp -r $folder/* testcase/
    cd testcase/
    javac -g *.java
    cd ..
    echo "####################################################################### $folder"
    output=$(timeout 5m java -cp .:../soot.jar PA2 | grep -v "^Soot ")
    echo "Your Output:"
    echo "$output"
    expected=$(cat testcase/answer/asc/Test*)
    echo "Expected Output:"
    echo "$expected"
    if [ "$output" == "$expected" ]; then
        echo "############################################################# PASSED"
        passed=$((passed+1))
    else
        echo "############################################################# FAILED"
    fi
    total=$((total+1))
    rm -rf testcase
done

echo "########################################################################### RESULTS"
echo "Total: $total"
echo "Passed: $passed"

rm -rf *.class */*.class
cd ..
