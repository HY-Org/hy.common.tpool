

cd .\bin

rd /s/q .\org\hy\common\cache\junit
rd /s/q .\org\hy\common\thread\junit

jar cvfm hy.common.tpool.jar MANIFEST.MF LICENSE org

copy hy.common.tpool.jar ..
del /q hy.common.tpool.jar
cd ..

