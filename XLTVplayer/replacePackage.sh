#!/bin/sh

// 新包名
np=$2
// 旧包名
op=$1

find src/ -name "*.java" -exec sed -ig "s/${op}.R/${np}.R/g" {} \;
sed -ig "s/package=\"${op}\"/package=\"${np}\"/g" AndroidManifest.xml
find ./ -name "*.javag" -or -name "*.xmlg" | xargs rm -rf

if [ $? == 0 ]
then
    echo "Success"
else
    echo "Fail"
fi