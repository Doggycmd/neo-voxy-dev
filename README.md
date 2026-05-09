# Neo-Voxy REFORGED 5/9/26 - SERVER -> CLIENT LOD

this very unfinished port of neovoxy is beginning to reach a point where it will be released with full neoforge compat and server to client lod propogation. this was a great starting point that helped some people https://modrinth.com/plugin/voxy-server-side port to a fabric version of LOD streaming. which of course now im porting back to my neoforge build.

we are having success and i see it releasing tomorrow 5/9/26 - get ready for a new era of neoforge.

tons of thanks to mc/cortex i know im not loved by u but i love u

xantha for making something of my spaghetti code.
neovoxy reforged 5/9


------------------------------
> ⚠️ **UNFINISHED PORT** - This is an experimental NeoForge port of the original Fabric Voxy mod. **Use at your own risk!**

Neo-Voxy is a NeoForge port of the Voxy mod, a far-distance rendering mod utilizing LODs (Level of Detail) for massive render distances. This port is currently in very early alpha and should be considered unstable.

## Current State

| Feature | Status |
|---------|--------|
| LOD Streaming | ⚠️  Functional/Experimental |
| Server-to-Client Sync | ✅ Functional |
| Shader Support (Iris) | ✅ Voxy Shaders Functional |
| Testing | ❌Not Done Rigorously|

### What Works
- Basic LOD streaming from server to client is functional
- Section serialization and network transfer works
- Block ID remapping between server and client
- Core rendering pipeline with Sodium integration
- Iris shader support expected to function (mixins are present)

### What's Broken/Incomplete
- **LOD streaming** is only partially working - some edge cases may fail
- No support for Flashback, Nvidium, or Chunky integrations
- General instability and lack of comprehensive testing

## Requirements

| Dependency | Required Version |
|------------|------------------|
| Minecraft | `1.21.1` |
| NeoForge | `21.1.77+` |
| Sodium | `0.6.0+` (NeoForge edition) |
| Iris | Required (NeoForge edition) |

## Installation

1. Install NeoForge 21.1.77 or later for Minecraft 1.21.1
2. Install Sodium for NeoForge (version 0.6.0+)
3. Install Iris Shaders for NeoForge
4. Place the Neo-Voxy jar in your mods folder

## Removed Mixins

The following mixins from the original Fabric Voxy have been **removed** in this NeoForge port and may be added in the future:

### Flashback Integration (Removed)
| Mixin | Purpose |
|-------|---------|
| `flashback.MixinFlashbackMeta` | Flashback recording metadata integration |
| `flashback.MixinFlashbackRecorder` | Flashback recording system integration |

### Nvidium Integration (Removed)
| Mixin | Purpose |
|-------|---------|
| `nvidium.MixinRenderPipeline` | Nvidium render pipeline compatibility |

### Chunky Integration (Removed)
| Mixin | Purpose |
|-------|---------|
| `chunky.MixinFabricWorld` | Chunky pregenerator integration |

### Other Removed Mixins
| Mixin | Purpose |
|-------|---------|
| `iris.MixinStandardMacros` | Iris shader macro definitions |
| `minecraft.MixinBlockableEventLoop` | Client thread event loop hooks |
| `minecraft.MixinGlDebug` | OpenGL debugging utilities |

> **Note**: These mixins were removed due to incompatibility with NeoForge, missing dependencies, or not being ported yet. Future versions may restore some of this functionality.

## Current Mixin Configuration

### Client Mixins (`voxy.mixins.json`)

**Minecraft Core (13 mixins)**
- `MixinWorld`, `MixinClientChunkCache`, `MixinClientCommonPacketListenerImpl`
- `MixinClientLevel`, `MixinClientPacketListener`, `MixinFogRenderer`
- `MixinLevelRenderer`, `MixinMinecraft`, `MixinRenderSystem`
- `MixinWindow`, `MixinLayerLightSectionStorage`

**Sodium Integration (7 mixins)**
- `AccessorChunkTracker`, `AccessorSodiumWorldRenderer`
- `MixinChunkJobQueue`, `MixinDefaultChunkRenderer`
- `MixinRenderSectionManager`, `MixinSodiumOptionsGUI`, `MixinSodiumWorldRenderer`

**Iris Integration (10 mixins)**
- `CustomUniformsAccessor`, `IrisRenderingPipelineAccessor`
- `MixinIris`, `MixinIrisRenderingPipeline`, `MixinIrisSamplers`
- `MixinLevelRenderer`, `MixinMatrixUniforms`
- `MixinPackRenderTargetDirectives`, `MixinProgramSet`, `MixinShaderPackSourceNames`

### Common Mixins (`voxy-common.mixins.json`)
- `MixinLevelCommon` - Server/common level hooks

## Known Issues
> ⚠️ **INCOMPATABILITIES**: BetterFpsDist.


## Contributing

This is an unofficial port. If you encounter issues:
1. First check if the issue exists in the original Fabric version
2. If it's port-specific, document the issue with steps to reproduce
3. PRs are welcome for fixing NeoForge compatibility issues

## Credits

- **Original Voxy Mod**: [cortex](https://github.com/cortex/voxy)
- **NeoForge Port**: Community effort

## License

```
Copyright 2025 MCRcortex
All rights reserved. Do not redistribute.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
```

> ⚠️ **IMPORTANT**: This is a source-code-only fork. **Do not distribute compiled builds.** You must build from source yourself


---

**Version**: `0.2.0`  
**Minecraft**: `1.21.1`  
**Mod Loader**: NeoForge `21.1.77+`
