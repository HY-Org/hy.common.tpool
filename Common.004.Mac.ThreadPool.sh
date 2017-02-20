#!/bin/sh

cd bin

rm -R ./com
rm -R ./org/apache


rm -R ./org/hy/common/app
rm -R ./org/hy/common/cache/junit
rm -R ./org/hy/common/configfile
rm -R ./org/hy/common/db
rm -R ./org/hy/common/file
rm -R ./org/hy/common/ftp
rm -R ./org/hy/common/mail
rm -R ./org/hy/common/net
rm -R ./org/hy/common/logdb
rm -R ./org/hy/common/security
rm -R ./org/hy/common/thread/junit
rm -R ./org/hy/common/ui
rm -R ./org/hy/common/xml


rm    ./org/hy/common/*


jar cvfm hy.common.tpool.jar Common.ThreadPool.MANIFEST.MF org
cp hy.common.tpool.jar ..


rm hy.common.tpool.jar
cd ..

