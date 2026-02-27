$pomPath = "pom.xml"
$targetDir = ".."                            # 要替换的目标目录
$targetArtifactId = "hy.common.tpool"        # 要匹配的artifactId
$currentDir = $PSScriptRoot                  # 当前目录
$currentDirPrefix = "$currentDir\"           # 拼接路径前缀（结尾加\，确保匹配“当前目录/子目录”的格式）
$newVersion = ""                             # 新版本号



# 检查文件是否存在
if (Test-Path $pomPath) {
    # 加载XML文件并解析版本号
    $pomXml = [xml](Get-Content $pomPath -Encoding UTF8)
    $newVersion = $pomXml.project.version
    
    # 显示版本号
    if ($newVersion) {
        Write-Host "$targetArtifactId version: $newVersion" -ForegroundColor Green
    } else {
        # 兼容父模块依赖的情况（version可能在parent节点）
        $parentVersion = $pomXml.project.parent.version
        if ($parentVersion) {
            Write-Host "pom.xml parentVersion: $parentVersion" -ForegroundColor Green
            return
        } else {
            Write-Host "not find pom version node" -ForegroundColor Red
            return
        }
    }
} else {
    Write-Host "not find pom: $pomPath" -ForegroundColor Red
    return
}



# ========== 批量更新目标pom.xml文件 ==========
# 递归查找目标目录下所有pom.xml（排除源pom.xml，避免自更新）
$targetPomFiles = Get-ChildItem -Path $targetDir -Filter "pom.xml" -Recurse | 
    Where-Object { 
        # 条件1：排除源pom.xml文件
        $_.FullName -ne $sourcePomPath -and 
        # 条件2：排除当前目录下的所有pom.xml
        $_.FullName -notlike "$currentDirPrefix*" -and 
        $_.FullName -notlike "*hy.common.net*"
    }

if ($targetPomFiles.Count -eq 0) {
    Write-Host "not any pom.xml" -ForegroundColor Yellow
    return
}

# 遍历每个目标pom.xml
foreach ($pomFile in $targetPomFiles) {
    try {
        # 读取文件内容（Raw模式保留格式，避免XML解析破坏缩进）
        $content = Get-Content -Path $pomFile.FullName -Encoding UTF8 -Raw

        # 正则匹配：<artifactId>...</artifactId> 后紧跟的 <version>...</version>
        # 兼容换行/空格/缩进的多种格式
        $pattern = '(?s)(?<=<artifactId>' + $targetArtifactId + '</artifactId>\s*<version>)([^<]+)(?=</version>)'

        if ($content -match $pattern) {
            Write-Host "`nFinding: $($pomFile.FullName)" -ForegroundColor Cyan
            $oldVersion = $matches[1]  # 提取旧版本号
            if ($oldVersion -ne $newVersion) {
                # 直接替换匹配到的旧版本号为新版本号（无任何$分组引用）
                $content = $content -replace $pattern, $newVersion
                $content = $content.TrimEnd("`r", "`n", " ")          # 去除末尾的回车、换行、空格
                Set-Content $pomFile.FullName -Value $content -Encoding UTF8
                Write-Host "Updated: $oldVersion -> $newVersion" -ForegroundColor Green
            } else {
                Write-Host "Skipped: No change (version already $oldVersion)" -ForegroundColor Gray
            }
        }
        else {
            # Write-Host "Skipped: Not find" -ForegroundColor Gray
        }
    }
    catch {
        Write-Host "Error  : $($_.Exception.Message)" -ForegroundColor Red
    }
}