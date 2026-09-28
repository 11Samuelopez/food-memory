# Food Memory design tokens

Visual values live in `ui/theme/Color.kt` and `ui/theme/DesignTokens.kt`. Feature screens should use these shared tokens so a future design adjustment happens in one place. The values below preserve the current interface.

## Colors

| Token | Hex | Use |
| --- | --- | --- |
| `FoodForest` | `#244A3D` | Primary actions and dark brand surfaces |
| `FoodTerracotta` | `#D96C4A` | Accent and food/photo highlights |
| `FoodCream` | `#FFFFFF` | Main light surface |
| `FoodPeach` | `#FFE4D7` | Warm secondary surface |
| `FoodSage` | `#E1EBDD` | Positive/soft secondary surface |
| `FoodInk` | `#292A28` | Main text |
| `FoodMuted` | `#777873` | Secondary text |
| `FoodSurfaceVariant` | `#F4F6F4` | Material secondary surface |
| `FoodOutlineVariant` | `#E2E7E3` | Material dividers and borders |
| `FoodProfileCard` | `#F4F7F3` | Profile card |
| `FoodProfileBorder` | `#E5EBE5` | Profile card outline |
| `FoodMapBackground` | `#F2F5F2` | Map loading/empty background |

## Dimensions

`FoodSpacing` defines the shared 4, 8, 13, 16, 18, 22, 24, 28 and 32 dp spacing values. `FoodCorners` defines 12, 16, 18, 20, 22, 24, 26 and 28 dp corner families. `FoodSizes` centralizes the 24 dp icon, 48 dp touch target and 58 dp profile avatar. These are code constants rather than remote or runtime-generated values. Feature screens should prefer the closest named token and introduce a named token when a new repeated value appears.
