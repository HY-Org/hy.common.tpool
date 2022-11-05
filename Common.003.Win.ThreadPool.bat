

del /Q hy.common.tpool.jar
del /Q hy.common.tpool-sources.jar


call mvn clean package
cd .\target\classes


rd /s/q .\org\hy\common\cache\junit
rd /s/q .\org\hy\common\thread\junit

jar cvfm hy.common.tpool.jar META-INF/MANIFEST.MF META-INF org

copy hy.common.tpool.jar ..\..
del /q hy.common.tpool.jar
cd ..\..





cd .\src\main\java
xcopy /S ..\resources\* .
jar cvfm hy.common.tpool-sources.jar META-INF\MANIFEST.MF META-INF org 
copy hy.common.tpool-sources.jar ..\..\..
del /Q hy.common.tpool-sources.jar
rd /s/q META-INF
cd ..\..\..
