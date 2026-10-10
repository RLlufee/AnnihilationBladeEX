param(
    [Parameter(Mandatory = $true)]
    [string]$JarPath
)

# 从成品 Jar 检查资源引用；Java 编译成功无法证明模型、贴图、音效已打包。
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $JarPath).Path)
try {
    $required = @(
        'assets/annihilationblade/model/loli_blade.obj',
        'assets/annihilationblade/model/loli_blade.png',
        'assets/annihilationblade/blade_desc/loli_blade.json',
        'assets/annihilationblade/sounds.json',
        'assets/annihilationblade/sounds/loli_success.ogg',
        'data/annihilationblade/recipes/annihilationblade.loli_blade.json',
        'data/annihilationblade/slashblade/named_blades/loli_blade.json'
    )
    function Assert-Resource([string]$Name) {
        $entry = $archive.GetEntry($Name)
        if ($null -eq $entry -or $entry.Length -eq 0) {
            throw "Jar 缺少资源或资源为空：$Name"
        }
    }
    function Read-Resource([string]$Name) {
        Assert-Resource $Name
        $reader = [System.IO.StreamReader]::new($archive.GetEntry($Name).Open(), [System.Text.Encoding]::UTF8)
        try { return $reader.ReadToEnd() } finally { $reader.Dispose() }
    }
    foreach ($name in $required) { Assert-Resource $name }

    $blades = @($archive.Entries | Where-Object {
        $_.FullName -match '^data/annihilationblade/slashblade/named_blades/[^/]+\.json$'
    })
    foreach ($entry in $blades) {
        $blade = Read-Resource $entry.FullName | ConvertFrom-Json
        foreach ($reference in @($blade.render.model, $blade.render.texture)) {
            if ($reference -like 'annihilationblade:*') {
                Assert-Resource ('assets/annihilationblade/' + $reference.Split(':', 2)[1])
            }
        }
    }
    $soundMap = Read-Resource 'assets/annihilationblade/sounds.json' | ConvertFrom-Json -AsHashtable
    if (-not $soundMap.ContainsKey('loli_success')) { throw '缺少萝莉刀音效注册映射' }
    foreach ($sound in $soundMap.Values) {
        foreach ($reference in $sound.sounds) {
            $name = if ($reference -is [string]) { $reference } else { $reference.name }
            if ($name -like 'annihilationblade:*' -and $reference.type -ne 'event') {
                Assert-Resource ('assets/annihilationblade/sounds/' + $name.Split(':', 2)[1] + '.ogg')
            }
        }
    }
    foreach ($locale in @('zh_cn', 'en_us', 'zh_tw', 'zh_hk')) {
        $lang = Read-Resource "assets/annihilationblade/lang/$locale.json" | ConvertFrom-Json -AsHashtable
        foreach ($key in @('item.annihilationblade.loli_blade', 'subtitle.annihilationblade.loli_success', 'jei.annihilationblade.blade_desc.loli_blade')) {
            if (-not $lang.ContainsKey($key)) { throw "$locale 缺少语言键：$key" }
        }
    }
    $obj = Read-Resource 'assets/annihilationblade/model/loli_blade.obj'
    foreach ($group in @('blade', 'sheath')) {
        if ($obj -notmatch "(?m)^g\s+$group\s*$") { throw "萝莉刀模型缺少渲染分组：$group" }
    }
    $pngStream = $archive.GetEntry('assets/annihilationblade/model/loli_blade.png').Open()
    try {
        $signature = [byte[]]::new(8)
        $read = $pngStream.Read($signature, 0, 8)
        if ($read -ne 8 -or [Convert]::ToHexString($signature) -ne '89504E470D0A1A0A') {
            throw '萝莉刀贴图不是有效的 PNG 文件'
        }
    } finally { $pngStream.Dispose() }
    Write-Output "资源校验通过：$($blades.Count) 把命名刀的本模组模型/贴图引用、萝莉刀资源、四语言与音效。"
} finally { $archive.Dispose() }
