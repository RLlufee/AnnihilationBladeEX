$ErrorActionPreference = 'Stop'
$OutputEncoding = [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)

Add-Type -AssemblyName System.Drawing

$projectRoot = Split-Path -Parent $PSScriptRoot
$modelDir = Join-Path $projectRoot 'src/main/resources/assets/annihilationblade/model'
$objPath = Join-Path $modelDir 'loli_blade.obj'
$texturePath = Join-Path $modelDir 'loli_blade.png'
$objBackupPath = Join-Path $modelDir 'loli_blade_before_charm.obj'
$manualTextureBasePath = Join-Path $modelDir 'loli_blade_manual_base.png'

if (-not (Test-Path -LiteralPath $objBackupPath)) {
    # 早期生成的无附加结构备份可能被清理。当前正式 OBJ 的新增面均使用更大的顶点索引，可据此恢复基准模型。
    $baseVertexCount = 2522
    $baseTextureCoordinateCount = 626
    $baseNormalCount = 1898
    $generatedGroupNames = @('blade', 'blade_luminous', 'item_blade', 'item_damaged', 'item_bladens')
    $currentLines = Get-Content -LiteralPath $objPath -Encoding UTF8
    $recoveredLines = [System.Collections.Generic.List[string]]::new()
    $vertexIndex = 0
    $textureCoordinateIndex = 0
    $normalIndex = 0
    $currentGroup = $null
    $atGeneratedGroupStart = $false

    foreach ($line in $currentLines) {
        if ($line -match '^v ') {
            $vertexIndex++
            if ($vertexIndex -le $baseVertexCount) {
                $recoveredLines.Add($line)
            }
            continue
        }
        if ($line -match '^vt ') {
            $textureCoordinateIndex++
            if ($textureCoordinateIndex -le $baseTextureCoordinateCount) {
                $recoveredLines.Add($line)
            }
            continue
        }
        if ($line -match '^vn ') {
            $normalIndex++
            if ($normalIndex -le $baseNormalCount) {
                $recoveredLines.Add($line)
            }
            continue
        }
        if ($line -match '^g\s+(\S+)') {
            $currentGroup = $Matches[1]
            $atGeneratedGroupStart = $generatedGroupNames -contains $currentGroup
            $recoveredLines.Add($line)
            continue
        }
        if ($atGeneratedGroupStart -and $line -eq 'usemtl mat1') {
            # 这是旧生成器在组首写入的材质行；与后续新增面一起去除。
            continue
        }
        if ($line -match '^f\s+') {
            $isGeneratedFace = $false
            foreach ($token in ($line -replace '^f\s+', '').Split(' ', [System.StringSplitOptions]::RemoveEmptyEntries)) {
                if ([int](($token -split '/')[0]) -gt $baseVertexCount) {
                    $isGeneratedFace = $true
                    break
                }
            }
            if ($isGeneratedFace) {
                continue
            }
            $atGeneratedGroupStart = $false
        }
        $recoveredLines.Add($line)
    }
    [System.IO.File]::WriteAllLines($objBackupPath, $recoveredLines, [System.Text.UTF8Encoding]::new($false))
}
if (-not (Test-Path -LiteralPath $manualTextureBasePath)) {
    # 用户手改后的正式贴图是后续生成的唯一底图，不能再回退到早期备份。
    Copy-Item -LiteralPath $texturePath -Destination $manualTextureBasePath
}

# 每次从无挂坠备份重建，避免重复执行时叠加模型数据。
# 旧脚本可能在基准末尾留下空 sheath 组；过滤它们后只在最终位置声明一套刀鞘。
$lines = @(Get-Content -LiteralPath $objBackupPath -Encoding UTF8 | Where-Object { $_ -notmatch '^g\s+(sheath|sheath_luminous)$' })
$vertexCount = ($lines | Where-Object { $_ -match '^v ' }).Count
$textureCoordinateCount = ($lines | Where-Object { $_ -match '^vt ' }).Count
$normalCount = ($lines | Where-Object { $_ -match '^vn ' }).Count

$newVertices = [System.Collections.Generic.List[string]]::new()
$newTextureCoordinates = [System.Collections.Generic.List[string]]::new()
$newNormals = [System.Collections.Generic.List[string]]::new()
$facesByGroup = @{
    blade = [System.Collections.Generic.List[string]]::new()
    blade_luminous = [System.Collections.Generic.List[string]]::new()
    sheath = [System.Collections.Generic.List[string]]::new()
    sheath_luminous = [System.Collections.Generic.List[string]]::new()
    item_blade = [System.Collections.Generic.List[string]]::new()
    item_damaged = [System.Collections.Generic.List[string]]::new()
    item_bladens = [System.Collections.Generic.List[string]]::new()
}
foreach ($faceList in $facesByGroup.Values) {
    [void]$faceList.Add('usemtl mat1')
}

function Add-Vertex {
    param([double]$X, [double]$Y, [double]$Z)
    [void]$newVertices.Add(('v {0:F6} {1:F6} {2:F6}' -f $X, $Y, $Z))
    return $vertexCount + $newVertices.Count
}

function Add-TextureCoordinate {
    param([double]$U, [double]$V)
    [void]$newTextureCoordinates.Add(('vt {0:F6} {1:F6}' -f $U, $V))
    return $textureCoordinateCount + $newTextureCoordinates.Count
}

function Add-Normal {
    param([double]$X, [double]$Y, [double]$Z)
    [void]$newNormals.Add(('vn {0:F6} {1:F6} {2:F6}' -f $X, $Y, $Z))
    return $normalCount + $newNormals.Count
}

function Add-Triangle {
    param(
        [string]$Group,
        [int]$A,
        [int]$B,
        [int]$C,
        [int]$TextureCoordinate,
        [int]$Normal
    )
    [void]$facesByGroup[$Group].Add(('f {0}/{3}/{4} {1}/{3}/{4} {2}/{3}/{4}' -f $A, $B, $C, $TextureCoordinate, $Normal))
}

$normalFront = Add-Normal -X 0 -Y 0 -Z 1
$normalBack = Add-Normal -X 0 -Y 0 -Z -1
$normalSide = Add-Normal -X 0 -Y 1 -Z 0

# 调色板从用户手改贴图取色，并仅写入透明空白区，原有 UV 岛不会被覆盖。
# 同时写入一份 V 轴翻转后的镜像调色板，避免 OBJ 贴图坐标在游戏渲染时采到透明区。
$palette = @(
    @{ Name = 'rose'; X = 868; Y = 23 },
    @{ Name = 'coral'; X = 279; Y = 719 },
    @{ Name = 'gold'; X = 274; Y = 589 },
    @{ Name = 'green'; X = 801; Y = 23 },
    @{ Name = 'cyan'; X = 303; Y = 725 },
    @{ Name = 'blue'; X = 777; Y = 46 },
    @{ Name = 'violet'; X = 672; Y = 692 },
    @{ Name = 'pink'; X = 897; Y = 47 },
    @{ Name = 'metalGold'; X = 372; Y = 197 },
    @{ Name = 'pearl'; X = 202; Y = 763 },
    @{ Name = 'dark'; X = 848; Y = 90 },
    @{ Name = 'wine'; X = 282; Y = 662 },
    @{ Name = 'lightRose'; X = 363; Y = 764 },
    @{ Name = 'lightCyan'; X = 284; Y = 731 }
)

$paletteTextureCoordinates = @{}
$paletteStartX = 612
$paletteY = 286
for ($i = 0; $i -lt $palette.Count; $i++) {
    $pixelX = $paletteStartX + $i * 10 + 4
    $pixelY = $paletteY + 4
    $u = $pixelX / 1023.0
    $v = $pixelY / 1023.0
    $paletteTextureCoordinates[$palette[$i].Name] = Add-TextureCoordinate $u $v
}

function Convert-ToItemPoint {
    param([double]$X, [double]$Y, [double]$Z)

    # 原 OBJ 的 item_blade 是 blade 的固定缩放旋转副本，参数由对应顶点反解。
    $a = 0.348742
    $b = 0.284551
    $tx = 41.119581
    $ty = 30.857565
    $scaleZ = 0.45
    $tz = 9.847191
    return @(
        ($a * $X - $b * $Y + $tx),
        ($b * $X + $a * $Y + $ty),
        ($scaleZ * $Z + $tz)
    )
}

function Add-PrismSegment {
    param(
        [string[]]$Groups,
        [double]$X1,
        [double]$Y1,
        [double]$X2,
        [double]$Y2,
        [double]$Width,
        [double]$Depth,
        [string]$ColorName,
        [double]$CenterZ = 0.0,
        [switch]$ItemSpace
    )

    $dx = $X2 - $X1
    $dy = $Y2 - $Y1
    $length = [Math]::Sqrt($dx * $dx + $dy * $dy)
    $px = -$dy / $length * $Width * 0.5
    $py = $dx / $length * $Width * 0.5
    $points = @(
        @(($X1 + $px), ($Y1 + $py), ($CenterZ + $Depth)),
        @(($X1 - $px), ($Y1 - $py), ($CenterZ + $Depth)),
        @(($X2 - $px), ($Y2 - $py), ($CenterZ + $Depth)),
        @(($X2 + $px), ($Y2 + $py), ($CenterZ + $Depth)),
        @(($X1 + $px), ($Y1 + $py), ($CenterZ - $Depth)),
        @(($X1 - $px), ($Y1 - $py), ($CenterZ - $Depth)),
        @(($X2 - $px), ($Y2 - $py), ($CenterZ - $Depth)),
        @(($X2 + $px), ($Y2 + $py), ($CenterZ - $Depth))
    )
    $indices = @()
    foreach ($point in $points) {
        if ($ItemSpace) {
            $point = Convert-ToItemPoint $point[0] $point[1] $point[2]
        }
        $indices += Add-Vertex $point[0] $point[1] $point[2]
    }
    $uv = $paletteTextureCoordinates[$ColorName]
    foreach ($group in $Groups) {
        Add-Triangle $group $indices[0] $indices[1] $indices[2] $uv $normalFront
        Add-Triangle $group $indices[0] $indices[2] $indices[3] $uv $normalFront
        Add-Triangle $group $indices[6] $indices[5] $indices[4] $uv $normalBack
        Add-Triangle $group $indices[7] $indices[6] $indices[4] $uv $normalBack
        Add-Triangle $group $indices[0] $indices[4] $indices[5] $uv $normalSide
        Add-Triangle $group $indices[0] $indices[5] $indices[1] $uv $normalSide
        Add-Triangle $group $indices[3] $indices[2] $indices[6] $uv $normalSide
        Add-Triangle $group $indices[3] $indices[6] $indices[7] $uv $normalSide
    }
}

function Add-DiamondCrystal {
    param(
        [string[]]$Groups,
        [double]$CenterX,
        [double]$CenterY,
        [double]$SizeX,
        [double]$SizeY,
        [double]$CenterZ = 0.0,
        [double]$Depth = 0.70,
        [string]$FirstColor = 'pink',
        [string]$SecondColor = 'cyan',
        [switch]$ItemSpace
    )

    $rawPoints = @(
        @($CenterX, ($CenterY + $SizeY), $CenterZ),
        @(($CenterX + $SizeX), $CenterY, $CenterZ),
        @($CenterX, ($CenterY - $SizeY), $CenterZ),
        @(($CenterX - $SizeX), $CenterY, $CenterZ),
        @($CenterX, $CenterY, ($CenterZ + $Depth)),
        @($CenterX, $CenterY, ($CenterZ - $Depth))
    )
    $indices = @()
    foreach ($point in $rawPoints) {
        if ($ItemSpace) {
            $point = Convert-ToItemPoint $point[0] $point[1] $point[2]
        }
        $indices += Add-Vertex $point[0] $point[1] $point[2]
    }
    $firstUv = $paletteTextureCoordinates[$FirstColor]
    $secondUv = $paletteTextureCoordinates[$SecondColor]
    foreach ($group in $Groups) {
        Add-Triangle $group $indices[0] $indices[1] $indices[4] $firstUv $normalFront
        Add-Triangle $group $indices[1] $indices[2] $indices[4] $secondUv $normalFront
        Add-Triangle $group $indices[2] $indices[3] $indices[4] $firstUv $normalFront
        Add-Triangle $group $indices[3] $indices[0] $indices[4] $secondUv $normalFront
        Add-Triangle $group $indices[1] $indices[0] $indices[5] $firstUv $normalBack
        Add-Triangle $group $indices[2] $indices[1] $indices[5] $secondUv $normalBack
        Add-Triangle $group $indices[3] $indices[2] $indices[5] $firstUv $normalBack
        Add-Triangle $group $indices[0] $indices[3] $indices[5] $secondUv $normalBack
    }
}

function Add-HandleDetails {
    param([string[]]$SolidGroups, [string[]]$LuminousGroups, [switch]$ItemSpace)

    # 刀柄沿 +X 延伸。交错细带保留酒红底色，只以粉青金三色提亮边缘。
    $wrapCenters = @(18.0, 24.0, 30.0, 36.0, 42.0, 48.0, 54.0)
    $wrapColors = @('lightRose', 'lightCyan', 'metalGold', 'lightRose', 'lightCyan', 'metalGold', 'lightRose')
    for ($i = 0; $i -lt $wrapCenters.Count; $i++) {
        $x = $wrapCenters[$i]
        $centerY = 6.55 - 0.048 * $x
        Add-PrismSegment -Groups $SolidGroups -X1 ($x - 1.45) -Y1 ($centerY + 1.85) -X2 ($x + 1.45) -Y2 ($centerY - 1.85) -Width 0.34 -Depth 0.24 -CenterZ 0.98 -ColorName $wrapColors[$i] -ItemSpace:$ItemSpace
        Add-PrismSegment -Groups $SolidGroups -X1 ($x - 1.45) -Y1 ($centerY - 1.85) -X2 ($x + 1.45) -Y2 ($centerY + 1.85) -Width 0.24 -Depth 0.20 -CenterZ 0.98 -ColorName 'wine' -ItemSpace:$ItemSpace
    }

    # 柄尾束带以金属压住收口，中心的小晶体与挂坠的发光核心呼应。
    Add-PrismSegment -Groups $SolidGroups -X1 58.2 -Y1 0.70 -X2 58.2 -Y2 6.42 -Width 1.05 -Depth 0.42 -CenterZ 0.98 -ColorName 'metalGold' -ItemSpace:$ItemSpace
    Add-PrismSegment -Groups $SolidGroups -X1 59.35 -Y1 1.25 -X2 59.35 -Y2 5.88 -Width 0.46 -Depth 0.30 -CenterZ 1.12 -ColorName 'wine' -ItemSpace:$ItemSpace
    Add-DiamondCrystal -Groups $LuminousGroups -CenterX 59.05 -CenterY 3.54 -SizeX 1.10 -SizeY 1.55 -CenterZ 1.48 -Depth 0.48 -FirstColor 'pink' -SecondColor 'cyan' -ItemSpace:$ItemSpace
}

function Add-GuardDetails {
    param([string[]]$SolidGroups, [string[]]$LuminousGroups, [switch]$ItemSpace)

    # 护手做成短小的双翼，不改变原模型主体轮廓。
    Add-PrismSegment -Groups $SolidGroups -X1 3.2 -Y1 3.55 -X2 8.4 -Y2 10.10 -Width 1.85 -Depth 0.34 -CenterZ 0.32 -ColorName 'lightRose' -ItemSpace:$ItemSpace
    Add-PrismSegment -Groups $SolidGroups -X1 3.2 -Y1 3.55 -X2 8.4 -Y2 -3.00 -Width 1.85 -Depth 0.34 -CenterZ 0.32 -ColorName 'lightCyan' -ItemSpace:$ItemSpace
    Add-PrismSegment -Groups $SolidGroups -X1 3.8 -Y1 3.55 -X2 7.35 -Y2 8.95 -Width 0.46 -Depth 0.44 -CenterZ 0.68 -ColorName 'metalGold' -ItemSpace:$ItemSpace
    Add-PrismSegment -Groups $SolidGroups -X1 3.8 -Y1 3.55 -X2 7.35 -Y2 -1.85 -Width 0.46 -Depth 0.44 -CenterZ 0.68 -ColorName 'metalGold' -ItemSpace:$ItemSpace
    Add-DiamondCrystal -Groups $LuminousGroups -CenterX 8.8 -CenterY 10.55 -SizeX 1.15 -SizeY 1.55 -CenterZ 0.82 -Depth 0.48 -FirstColor 'pink' -SecondColor 'cyan' -ItemSpace:$ItemSpace
    Add-DiamondCrystal -Groups $LuminousGroups -CenterX 8.8 -CenterY -3.45 -SizeX 1.15 -SizeY 1.55 -CenterZ 0.82 -Depth 0.48 -FirstColor 'cyan' -SecondColor 'pink' -ItemSpace:$ItemSpace
}

function Add-BladeSpineGlow {
    param([string[]]$Groups, [switch]$ItemSpace)

    # 只沿刀脊保留六段窄光痕，彩虹被压缩成远看一条能量刃纹的节奏。
    $segments = @(
        @{ X1 = -287.0; Y1 = -4.0; X2 = -268.0; Y2 = 3.55; Color = 'rose'; Z = -4.75 },
        @{ X1 = -266.0; Y1 = 4.25; X2 = -244.0; Y2 = 8.45; Color = 'gold'; Z = -4.10 },
        @{ X1 = -242.0; Y1 = 9.15; X2 = -216.0; Y2 = 11.15; Color = 'green'; Z = -3.25 },
        @{ X1 = -214.0; Y1 = 11.70; X2 = -184.0; Y2 = 12.80; Color = 'cyan'; Z = -2.75 },
        @{ X1 = -182.0; Y1 = 13.10; X2 = -151.0; Y2 = 12.85; Color = 'blue'; Z = -2.45 },
        @{ X1 = -149.0; Y1 = 12.85; X2 = -119.0; Y2 = 12.30; Color = 'violet'; Z = -1.15 }
    )
    foreach ($segment in $segments) {
        Add-PrismSegment -Groups $Groups -X1 $segment.X1 -Y1 $segment.Y1 -X2 $segment.X2 -Y2 $segment.Y2 -Width 0.62 -Depth 0.22 -CenterZ $segment.Z -ColorName $segment.Color -ItemSpace:$ItemSpace
    }
}

function Add-BladeRootCore {
    param([string[]]$Groups, [switch]$ItemSpace)

    # 刀镡与主刀身之间的窄过渡段：三枚结晶形成发光刀根，细翼片收住光线。
    Add-DiamondCrystal -Groups $Groups -CenterX -21.0 -CenterY 4.78 -SizeX 2.15 -SizeY 1.70 -CenterZ 3.86 -Depth 0.54 -FirstColor 'pink' -SecondColor 'cyan' -ItemSpace:$ItemSpace
    Add-DiamondCrystal -Groups $Groups -CenterX -14.25 -CenterY 4.78 -SizeX 0.98 -SizeY 1.08 -CenterZ 3.78 -Depth 0.36 -FirstColor 'lightRose' -SecondColor 'cyan' -ItemSpace:$ItemSpace
    Add-DiamondCrystal -Groups $Groups -CenterX -27.75 -CenterY 4.78 -SizeX 0.98 -SizeY 1.08 -CenterZ 3.78 -Depth 0.36 -FirstColor 'cyan' -SecondColor 'lightRose' -ItemSpace:$ItemSpace
    Add-PrismSegment -Groups $Groups -X1 -28.85 -Y1 6.80 -X2 -20.90 -Y2 7.95 -Width 0.30 -Depth 0.20 -CenterZ 3.70 -ColorName 'lightRose' -ItemSpace:$ItemSpace
    Add-PrismSegment -Groups $Groups -X1 -28.85 -Y1 2.76 -X2 -20.90 -Y2 1.61 -Width 0.30 -Depth 0.20 -CenterZ 3.70 -ColorName 'lightCyan' -ItemSpace:$ItemSpace
    Add-PrismSegment -Groups $Groups -X1 -21.10 -Y1 7.95 -X2 -13.15 -Y2 6.80 -Width 0.30 -Depth 0.20 -CenterZ 3.70 -ColorName 'lightCyan' -ItemSpace:$ItemSpace
    Add-PrismSegment -Groups $Groups -X1 -21.10 -Y1 1.61 -X2 -13.15 -Y2 2.76 -Width 0.30 -Depth 0.20 -CenterZ 3.70 -ColorName 'lightRose' -ItemSpace:$ItemSpace
    Add-PrismSegment -Groups $Groups -X1 -29.30 -Y1 4.78 -X2 -12.70 -Y2 4.78 -Width 0.22 -Depth 0.17 -CenterZ 3.58 -ColorName 'metalGold' -ItemSpace:$ItemSpace
}

function Add-ClosedRibbon {
    param(
        [string[]]$Groups,
        [object[]]$Centerline,
        [double[]]$Widths,
        [double]$Depth,
        [string]$ColorName,
        [double]$CenterZ = 0.0
    )

    if ($Centerline.Count -lt 2 -or $Centerline.Count -ne $Widths.Count) {
        throw '封闭刀鞘轮廓至少需要两个与宽度一一对应的控制点。'
    }

    $nodeIndices = [System.Collections.Generic.List[object]]::new()
    for ($i = 0; $i -lt $Centerline.Count; $i++) {
        $previous = $Centerline[[Math]::Max(0, $i - 1)]
        $next = $Centerline[[Math]::Min($Centerline.Count - 1, $i + 1)]
        $dx = $next.X - $previous.X
        $dy = $next.Y - $previous.Y
        $length = [Math]::Sqrt($dx * $dx + $dy * $dy)
        $offsetX = -$dy / $length * $Widths[$i] * 0.5
        $offsetY = $dx / $length * $Widths[$i] * 0.5
        $frontZ = $CenterZ + $Depth
        $backZ = $CenterZ - $Depth
        $nodeIndices.Add(@(
            (Add-Vertex ($Centerline[$i].X + $offsetX) ($Centerline[$i].Y + $offsetY) $frontZ),
            (Add-Vertex ($Centerline[$i].X - $offsetX) ($Centerline[$i].Y - $offsetY) $frontZ),
            (Add-Vertex ($Centerline[$i].X - $offsetX) ($Centerline[$i].Y - $offsetY) $backZ),
            (Add-Vertex ($Centerline[$i].X + $offsetX) ($Centerline[$i].Y + $offsetY) $backZ)
        ))
    }

    $uv = $paletteTextureCoordinates[$ColorName]
    foreach ($group in $Groups) {
        for ($i = 0; $i -lt ($nodeIndices.Count - 1); $i++) {
            $start = $nodeIndices[$i]
            $end = $nodeIndices[$i + 1]
            Add-Triangle $group $start[0] $start[1] $end[1] $uv $normalFront
            Add-Triangle $group $start[0] $end[1] $end[0] $uv $normalFront
            Add-Triangle $group $start[3] $end[2] $start[2] $uv $normalBack
            Add-Triangle $group $start[3] $end[3] $end[2] $uv $normalBack
            Add-Triangle $group $start[0] $end[0] $end[3] $uv $normalSide
            Add-Triangle $group $start[0] $end[3] $start[3] $uv $normalSide
            Add-Triangle $group $start[1] $end[2] $end[1] $uv $normalSide
            Add-Triangle $group $start[1] $start[2] $end[2] $uv $normalSide
        }
        $first = $nodeIndices[0]
        $last = $nodeIndices[$nodeIndices.Count - 1]
        Add-Triangle $group $first[0] $first[2] $first[1] $uv $normalSide
        Add-Triangle $group $first[0] $first[3] $first[2] $uv $normalSide
        Add-Triangle $group $last[0] $last[1] $last[2] $uv $normalSide
        Add-Triangle $group $last[0] $last[2] $last[3] $uv $normalSide
    }
}

function Add-SheathRainbowEmblem {
    param([string[]]$SolidGroups, [string[]]$LuminousGroups)

    # 鞘面标志放在较宽的中前段，尺寸控制在鞘壳边界内，避免插鞘显示时外扩遮挡刀身。
    $centerX = -246.0
    $centerY = 3.00
    $radius = 3.05
    $ringWidth = 0.58
    $colors = @('rose', 'coral', 'gold', 'green', 'cyan', 'blue', 'violet', 'pink')

    Add-PrismSegment -Groups $SolidGroups -X1 -249.65 -Y1 0.34 -X2 -242.35 -Y2 5.66 -Width 0.22 -Depth 0.11 -CenterZ 4.66 -ColorName 'metalGold'
    Add-PrismSegment -Groups $SolidGroups -X1 -249.65 -Y1 5.66 -X2 -242.35 -Y2 0.34 -Width 0.22 -Depth 0.11 -CenterZ 4.66 -ColorName 'metalGold'

    for ($segment = 0; $segment -lt 8; $segment++) {
        $angle1 = (-90 + $segment * 45) * [Math]::PI / 180.0
        $angle2 = (-90 + ($segment + 1) * 45) * [Math]::PI / 180.0
        $x1 = $centerX + [Math]::Cos($angle1) * $radius
        $y1 = $centerY + [Math]::Sin($angle1) * $radius
        $x2 = $centerX + [Math]::Cos($angle2) * $radius
        $y2 = $centerY + [Math]::Sin($angle2) * $radius
        Add-PrismSegment -Groups $LuminousGroups -X1 $x1 -Y1 $y1 -X2 $x2 -Y2 $y2 -Width $ringWidth -Depth 0.13 -CenterZ 4.82 -ColorName $colors[$segment]
    }

    Add-DiamondCrystal -Groups $LuminousGroups -CenterX $centerX -CenterY $centerY -SizeX 0.78 -SizeY 0.78 -CenterZ 4.94 -Depth 0.20 -FirstColor 'pink' -SecondColor 'cyan'
}

function Add-SheathSmallDecorations {
    param([string[]]$SolidGroups, [string[]]$LuminousGroups)

    # 细金线和小晶体按鞘体弧线分散摆放，补层次但不改变整体轮廓。
    Add-PrismSegment -Groups $SolidGroups -X1 -286.5 -Y1 -2.88 -X2 -274.0 -Y2 0.28 -Width 0.24 -Depth 0.10 -CenterZ 4.63 -ColorName 'metalGold'
    Add-PrismSegment -Groups $SolidGroups -X1 -263.0 -Y1 4.62 -X2 -252.0 -Y2 5.84 -Width 0.20 -Depth 0.10 -CenterZ 4.64 -ColorName 'pearl'
    Add-PrismSegment -Groups $SolidGroups -X1 -104.0 -Y1 8.42 -X2 -88.0 -Y2 7.20 -Width 0.22 -Depth 0.10 -CenterZ 4.64 -ColorName 'metalGold'
    Add-PrismSegment -Groups $SolidGroups -X1 -70.0 -Y1 2.70 -X2 -54.0 -Y2 3.25 -Width 0.20 -Depth 0.10 -CenterZ 4.64 -ColorName 'pearl'

    Add-DiamondCrystal -Groups $LuminousGroups -CenterX -274.5 -CenterY -0.32 -SizeX 0.72 -SizeY 0.58 -CenterZ 4.78 -Depth 0.18 -FirstColor 'lightRose' -SecondColor 'lightCyan'
    Add-DiamondCrystal -Groups $LuminousGroups -CenterX -222.0 -CenterY 6.08 -SizeX 0.62 -SizeY 0.54 -CenterZ 4.78 -Depth 0.17 -FirstColor 'cyan' -SecondColor 'pink'
    Add-DiamondCrystal -Groups $LuminousGroups -CenterX -93.5 -CenterY 5.72 -SizeX 0.70 -SizeY 0.58 -CenterZ 4.78 -Depth 0.18 -FirstColor 'pink' -SecondColor 'cyan'
    Add-DiamondCrystal -Groups $LuminousGroups -CenterX -55.5 -CenterY 4.82 -SizeX 0.58 -SizeY 0.50 -CenterZ 4.78 -Depth 0.16 -FirstColor 'lightCyan' -SecondColor 'lightRose'

    Add-PrismSegment -Groups $LuminousGroups -X1 -235.0 -Y1 0.36 -X2 -226.0 -Y2 1.72 -Width 0.16 -Depth 0.09 -CenterZ 4.72 -ColorName 'lightRose'
    Add-PrismSegment -Groups $LuminousGroups -X1 -233.5 -Y1 5.78 -X2 -224.5 -Y2 6.65 -Width 0.16 -Depth 0.09 -CenterZ 4.72 -ColorName 'lightCyan'
    Add-PrismSegment -Groups $LuminousGroups -X1 -116.0 -Y1 3.74 -X2 -106.5 -Y2 4.58 -Width 0.16 -Depth 0.09 -CenterZ 4.72 -ColorName 'rose'
    Add-PrismSegment -Groups $LuminousGroups -X1 -84.0 -Y1 7.92 -X2 -73.5 -Y2 7.50 -Width 0.16 -Depth 0.09 -CenterZ 4.72 -ColorName 'cyan'
}

function Add-LoliSheath {
    param([string[]]$SolidGroups, [string[]]$LuminousGroups)

    # 单体封闭鞘壳顺着原刀身弧线收束，避免分段棱柱在转折处开缝。
    $centerline = @(
        [pscustomobject]@{ X = -300.0; Y = -7.20 },
        [pscustomobject]@{ X = -278.0; Y = -1.00 },
        [pscustomobject]@{ X = -246.0; Y = 3.00 },
        [pscustomobject]@{ X = -210.0; Y = 6.00 },
        [pscustomobject]@{ X = -166.0; Y = 7.80 },
        [pscustomobject]@{ X = -122.0; Y = 7.40 },
        [pscustomobject]@{ X = -78.0; Y = 5.40 },
        [pscustomobject]@{ X = -35.0; Y = 4.50 },
        [pscustomobject]@{ X = -7.0; Y = 4.82 }
    )
    $bodyWidths = @(5.2, 6.2, 7.1, 7.6, 8.0, 8.5, 8.9, 9.3, 9.8)
    $seamWidths = @(0.42, 0.48, 0.52, 0.54, 0.56, 0.58, 0.60, 0.62, 0.64)
    Add-ClosedRibbon -Groups $SolidGroups -Centerline $centerline -Widths $bodyWidths -Depth 4.25 -ColorName 'wine'
    Add-ClosedRibbon -Groups $SolidGroups -Centerline $centerline -Widths $seamWidths -Depth 0.14 -CenterZ 4.38 -ColorName 'metalGold'

    # 四道束带与鞘面晶体全部跟随中线法向，装饰不会伸出鞘壳。
    $bindingIndices = @(2, 4, 6, 7)
    $bindingColors = @('metalGold', 'lightRose', 'metalGold', 'lightCyan')
    for ($i = 0; $i -lt $bindingIndices.Count; $i++) {
        $nodeIndex = $bindingIndices[$i]
        $previous = $centerline[$nodeIndex - 1]
        $next = $centerline[$nodeIndex + 1]
        $dx = $next.X - $previous.X
        $dy = $next.Y - $previous.Y
        $length = [Math]::Sqrt($dx * $dx + $dy * $dy)
        $normalX = -$dy / $length
        $normalY = $dx / $length
        $halfWidth = $bodyWidths[$nodeIndex] * 0.42
        $center = $centerline[$nodeIndex]
        Add-PrismSegment -Groups $SolidGroups -X1 ($center.X - $normalX * $halfWidth) -Y1 ($center.Y - $normalY * $halfWidth) -X2 ($center.X + $normalX * $halfWidth) -Y2 ($center.Y + $normalY * $halfWidth) -Width 0.58 -Depth 0.16 -CenterZ 4.42 -ColorName $bindingColors[$i]
    }

    Add-DiamondCrystal -Groups $LuminousGroups -CenterX -208.0 -CenterY 5.95 -SizeX 1.18 -SizeY 1.10 -CenterZ 4.72 -Depth 0.26 -FirstColor 'pink' -SecondColor 'cyan'
    Add-DiamondCrystal -Groups $LuminousGroups -CenterX -164.0 -CenterY 7.75 -SizeX 1.32 -SizeY 1.18 -CenterZ 4.72 -Depth 0.26 -FirstColor 'cyan' -SecondColor 'pink'
    Add-DiamondCrystal -Groups $LuminousGroups -CenterX -120.0 -CenterY 7.35 -SizeX 1.18 -SizeY 1.06 -CenterZ 4.72 -Depth 0.26 -FirstColor 'pink' -SecondColor 'cyan'
    Add-PrismSegment -Groups $LuminousGroups -X1 -190.0 -Y1 6.85 -X2 -176.0 -Y2 7.46 -Width 0.22 -Depth 0.12 -CenterZ 4.64 -ColorName 'lightCyan'
    Add-PrismSegment -Groups $LuminousGroups -X1 -150.0 -Y1 7.74 -X2 -136.0 -Y2 7.55 -Width 0.22 -Depth 0.12 -CenterZ 4.64 -ColorName 'rose'

    Add-SheathRainbowEmblem -SolidGroups $SolidGroups -LuminousGroups $LuminousGroups
    Add-SheathSmallDecorations -SolidGroups $SolidGroups -LuminousGroups $LuminousGroups

    # 鞘口装饰收进窄壳边界内，避免发光晶体从两侧伸出。
    Add-PrismSegment -Groups $SolidGroups -X1 -8.0 -Y1 -0.08 -X2 -8.0 -Y2 9.72 -Width 1.25 -Depth 4.48 -ColorName 'metalGold'
    Add-PrismSegment -Groups $SolidGroups -X1 -9.25 -Y1 0.45 -X2 -9.25 -Y2 9.20 -Width 0.40 -Depth 4.62 -ColorName 'dark'
    Add-DiamondCrystal -Groups $LuminousGroups -CenterX -14.15 -CenterY 4.80 -SizeX 1.45 -SizeY 1.42 -CenterZ 4.74 -Depth 0.30 -FirstColor 'pink' -SecondColor 'cyan'
    Add-PrismSegment -Groups $LuminousGroups -X1 -19.80 -Y1 6.95 -X2 -14.95 -Y2 8.22 -Width 0.24 -Depth 0.12 -CenterZ 4.67 -ColorName 'rose'
    Add-PrismSegment -Groups $LuminousGroups -X1 -19.80 -Y1 2.65 -X2 -14.95 -Y2 1.38 -Width 0.24 -Depth 0.12 -CenterZ 4.67 -ColorName 'cyan'
}

function Add-RainbowRing {
    param([string[]]$Groups, [switch]$ItemSpace)

    $centerX = 70.0
    $centerY = -6.0
    $outerRadius = 4.8
    $innerRadius = 3.55
    $depth = 0.38
    $colors = @('rose', 'coral', 'gold', 'green', 'cyan', 'blue', 'violet', 'pink')

    for ($segment = 0; $segment -lt 8; $segment++) {
        $angle1 = (-90 + $segment * 45) * [Math]::PI / 180.0
        $angle2 = (-90 + ($segment + 1) * 45) * [Math]::PI / 180.0
        $rawPoints = @(
            @(($centerX + [Math]::Cos($angle1) * $outerRadius), ($centerY + [Math]::Sin($angle1) * $outerRadius), $depth),
            @(($centerX + [Math]::Cos($angle2) * $outerRadius), ($centerY + [Math]::Sin($angle2) * $outerRadius), $depth),
            @(($centerX + [Math]::Cos($angle2) * $innerRadius), ($centerY + [Math]::Sin($angle2) * $innerRadius), $depth),
            @(($centerX + [Math]::Cos($angle1) * $innerRadius), ($centerY + [Math]::Sin($angle1) * $innerRadius), $depth),
            @(($centerX + [Math]::Cos($angle1) * $outerRadius), ($centerY + [Math]::Sin($angle1) * $outerRadius), (-$depth)),
            @(($centerX + [Math]::Cos($angle2) * $outerRadius), ($centerY + [Math]::Sin($angle2) * $outerRadius), (-$depth)),
            @(($centerX + [Math]::Cos($angle2) * $innerRadius), ($centerY + [Math]::Sin($angle2) * $innerRadius), (-$depth)),
            @(($centerX + [Math]::Cos($angle1) * $innerRadius), ($centerY + [Math]::Sin($angle1) * $innerRadius), (-$depth))
        )
        $indices = @()
        foreach ($point in $rawPoints) {
            if ($ItemSpace) {
                $point = Convert-ToItemPoint $point[0] $point[1] $point[2]
            }
            $indices += Add-Vertex $point[0] $point[1] $point[2]
        }
        $uv = $paletteTextureCoordinates[$colors[$segment]]
        foreach ($group in $Groups) {
            Add-Triangle $group $indices[0] $indices[1] $indices[2] $uv $normalFront
            Add-Triangle $group $indices[0] $indices[2] $indices[3] $uv $normalFront
            Add-Triangle $group $indices[6] $indices[5] $indices[4] $uv $normalBack
            Add-Triangle $group $indices[7] $indices[6] $indices[4] $uv $normalBack
            Add-Triangle $group $indices[0] $indices[4] $indices[5] $uv $normalSide
            Add-Triangle $group $indices[0] $indices[5] $indices[1] $uv $normalSide
            Add-Triangle $group $indices[3] $indices[2] $indices[6] $uv $normalSide
            Add-Triangle $group $indices[3] $indices[6] $indices[7] $uv $normalSide
        }
    }
}

function Add-Crystal {
    param([string[]]$Groups, [switch]$ItemSpace)

    $rawPoints = @(
        @(70.0, -1.9, 0.0),
        @(73.0, -6.0, 0.0),
        @(70.0, -10.1, 0.0),
        @(67.0, -6.0, 0.0),
        @(70.0, -6.0, 1.15),
        @(70.0, -6.0, -1.15)
    )
    $indices = @()
    foreach ($point in $rawPoints) {
        if ($ItemSpace) {
            $point = Convert-ToItemPoint $point[0] $point[1] $point[2]
        }
        $indices += Add-Vertex $point[0] $point[1] $point[2]
    }
    $pinkUv = $paletteTextureCoordinates['pink']
    $cyanUv = $paletteTextureCoordinates['cyan']
    foreach ($group in $Groups) {
        Add-Triangle $group $indices[0] $indices[1] $indices[4] $pinkUv $normalFront
        Add-Triangle $group $indices[1] $indices[2] $indices[4] $cyanUv $normalFront
        Add-Triangle $group $indices[2] $indices[3] $indices[4] $pinkUv $normalFront
        Add-Triangle $group $indices[3] $indices[0] $indices[4] $cyanUv $normalFront
        Add-Triangle $group $indices[1] $indices[0] $indices[5] $pinkUv $normalBack
        Add-Triangle $group $indices[2] $indices[1] $indices[5] $cyanUv $normalBack
        Add-Triangle $group $indices[3] $indices[2] $indices[5] $pinkUv $normalBack
        Add-Triangle $group $indices[0] $indices[3] $indices[5] $cyanUv $normalBack
    }
}

$worldGroups = @('blade')
$itemGroups = @('item_blade', 'item_damaged', 'item_bladens')
$worldLuminousGroups = @('blade_luminous')
$itemLuminousGroups = @('item_blade', 'item_damaged', 'item_bladens')
$sheathGroups = @('sheath')
$sheathLuminousGroups = @('sheath_luminous')

# 柄尾连接环、三段短链和两条不对称流苏。
Add-PrismSegment -Groups $worldGroups -X1 61.7 -Y1 3.6 -X2 63.5 -Y2 2.0 -Width 1.15 -Depth 0.45 -ColorName 'metalGold'
Add-PrismSegment -Groups $worldGroups -X1 63.5 -Y1 2.0 -X2 65.1 -Y2 0.7 -Width 0.78 -Depth 0.35 -ColorName 'metalGold'
Add-PrismSegment -Groups $worldGroups -X1 65.1 -Y1 0.7 -X2 66.5 -Y2 -1.4 -Width 0.72 -Depth 0.32 -ColorName 'pearl'
Add-PrismSegment -Groups $worldGroups -X1 66.5 -Y1 -1.4 -X2 66.7 -Y2 -2.6 -Width 0.68 -Depth 0.30 -ColorName 'metalGold'
Add-PrismSegment -Groups $worldGroups -X1 68.0 -Y1 -10.1 -X2 66.8 -Y2 -15.4 -Width 0.62 -Depth 0.25 -ColorName 'pink'
Add-PrismSegment -Groups $worldGroups -X1 72.0 -Y1 -10.1 -X2 73.8 -Y2 -14.2 -Width 0.62 -Depth 0.25 -ColorName 'cyan'
Add-RainbowRing -Groups $worldGroups
Add-Crystal -Groups @('blade_luminous')
Add-HandleDetails -SolidGroups $worldGroups -LuminousGroups $worldLuminousGroups
Add-GuardDetails -SolidGroups $worldGroups -LuminousGroups $worldLuminousGroups
Add-BladeSpineGlow -Groups $worldLuminousGroups
Add-BladeRootCore -Groups $worldLuminousGroups
Add-LoliSheath -SolidGroups $sheathGroups -LuminousGroups $sheathLuminousGroups

Add-PrismSegment -Groups $itemGroups -X1 61.7 -Y1 3.6 -X2 63.5 -Y2 2.0 -Width 1.15 -Depth 0.45 -ColorName 'metalGold' -ItemSpace
Add-PrismSegment -Groups $itemGroups -X1 63.5 -Y1 2.0 -X2 65.1 -Y2 0.7 -Width 0.78 -Depth 0.35 -ColorName 'metalGold' -ItemSpace
Add-PrismSegment -Groups $itemGroups -X1 65.1 -Y1 0.7 -X2 66.5 -Y2 -1.4 -Width 0.72 -Depth 0.32 -ColorName 'pearl' -ItemSpace
Add-PrismSegment -Groups $itemGroups -X1 66.5 -Y1 -1.4 -X2 66.7 -Y2 -2.6 -Width 0.68 -Depth 0.30 -ColorName 'metalGold' -ItemSpace
Add-PrismSegment -Groups $itemGroups -X1 68.0 -Y1 -10.1 -X2 66.8 -Y2 -15.4 -Width 0.62 -Depth 0.25 -ColorName 'pink' -ItemSpace
Add-PrismSegment -Groups $itemGroups -X1 72.0 -Y1 -10.1 -X2 73.8 -Y2 -14.2 -Width 0.62 -Depth 0.25 -ColorName 'cyan' -ItemSpace
Add-RainbowRing -Groups $itemGroups -ItemSpace
Add-Crystal -Groups $itemGroups -ItemSpace
Add-HandleDetails -SolidGroups $itemGroups -LuminousGroups $itemLuminousGroups -ItemSpace
Add-GuardDetails -SolidGroups $itemGroups -LuminousGroups $itemLuminousGroups -ItemSpace
Add-BladeSpineGlow -Groups $itemLuminousGroups -ItemSpace
Add-BladeRootCore -Groups $itemLuminousGroups -ItemSpace

$firstVt = ($lines | Select-String '^vt ' | Select-Object -First 1).LineNumber
$firstVn = ($lines | Select-String '^vn ' | Select-Object -First 1).LineNumber
$firstGroup = ($lines | Select-String '^g ' | Select-Object -First 1).LineNumber
$output = [System.Collections.Generic.List[string]]::new()

for ($lineNumber = 1; $lineNumber -le $lines.Count; $lineNumber++) {
    if ($lineNumber -eq $firstVt) {
        $output.AddRange($newVertices)
    }
    if ($lineNumber -eq $firstVn) {
        $output.AddRange($newTextureCoordinates)
    }
    if ($lineNumber -eq $firstGroup) {
        $output.AddRange($newNormals)
    }

    $line = $lines[$lineNumber - 1]
    $output.Add($line)
    if ($line -match '^g\s+(\S+)') {
        $groupName = $Matches[1]
        if ($facesByGroup.ContainsKey($groupName)) {
            $output.AddRange($facesByGroup[$groupName])
        }
    }
}

# 原模型没有鞘体组，需要在末尾显式声明，拔刀剑会按标准组名单独渲染它。
foreach ($groupName in @('sheath', 'sheath_luminous')) {
    if ($facesByGroup[$groupName].Count -gt 1) {
        $output.Add("g $groupName")
        $output.AddRange($facesByGroup[$groupName])
    }
}

[System.IO.File]::WriteAllLines($objPath, $output, [System.Text.UTF8Encoding]::new($false))

# 在新手改贴图基础上补充模型专用调色板。目标区域为透明空白区，不会触及现有图案。
$texture = [System.Drawing.Bitmap]::new($manualTextureBasePath)
$graphics = [System.Drawing.Graphics]::FromImage($texture)
try {
    $paletteMirrorY = ($texture.Height - 1) - ($paletteY + 4) - 4
    for ($i = 0; $i -lt $palette.Count; $i++) {
        $sampleColor = $texture.GetPixel($palette[$i].X, $palette[$i].Y)
        $brush = [System.Drawing.SolidBrush]::new($sampleColor)
        try {
            $graphics.FillRectangle($brush, $paletteStartX + $i * 10, $paletteY, 8, 8)
            $graphics.FillRectangle($brush, $paletteStartX + $i * 10, $paletteMirrorY, 8, 8)
        } finally {
            $brush.Dispose()
        }
    }
} finally {
    $graphics.Dispose()
}
$texture.Save($texturePath, [System.Drawing.Imaging.ImageFormat]::Png)
$texture.Dispose()

Write-Output ('已生成挂坠模型：{0}' -f $objPath)
Write-Output ('已在用户手改贴图基础上补充模型调色板：{0}' -f $texturePath)
Write-Output ('新增顶点：{0}，新增 UV：{1}，新增法线：{2}' -f $newVertices.Count, $newTextureCoordinates.Count, $newNormals.Count)
