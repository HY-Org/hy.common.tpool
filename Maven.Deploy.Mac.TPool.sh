#!/bin/sh

mvn deploy:deploy-file -Dfile=hy.common.tpool.jar                              -DpomFile=./src/META-INF/maven/org/hy/common/tpool/pom.xml -DrepositoryId=thirdparty -Durl=http://HY-ZhengWei:1481/repository/thirdparty
mvn deploy:deploy-file -Dfile=hy.common.tpool-sources.jar -Dclassifier=sources -DpomFile=./src/META-INF/maven/org/hy/common/tpool/pom.xml -DrepositoryId=thirdparty -Durl=http://HY-ZhengWei:1481/repository/thirdparty
