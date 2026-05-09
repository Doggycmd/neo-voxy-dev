package me.cortex.voxy.common.config.compressors;

import me.cortex.voxy.common.config.ConfigBuildCtx;
import me.cortex.voxy.common.util.MemoryBuffer;
import me.cortex.voxy.common.util.ThreadLocalMemoryBuffer;
import me.cortex.voxy.common.world.SaveLoadSystem;
import net.jpountz.lz4.LZ4Factory;
import me.cortex.voxy.common.util.UnsafeUtil;

public class LZ4Compressor implements StorageCompressor {
    private static final ThreadLocalMemoryBuffer SCRATCH = new ThreadLocalMemoryBuffer(
            SaveLoadSystem.BIGGEST_SERIALIZED_SECTION_SIZE + 1024);

    private final net.jpountz.lz4.LZ4Compressor compressor;
    private final net.jpountz.lz4.LZ4FastDecompressor decompressor;

    public LZ4Compressor() {
        this.decompressor = LZ4Factory.fastestInstance().fastDecompressor();
        this.compressor = LZ4Factory.fastestInstance().fastCompressor();
    }

    @Override
    public MemoryBuffer compress(MemoryBuffer saveData) {
        var res = new MemoryBuffer(this.compressor.maxCompressedLength((int) saveData.size) + 4);
        UnsafeUtil.memPutInt(res.address, (int) saveData.size);
        int size = this.compressor.compress(saveData.asByteBuffer(), 0, (int) saveData.size, res.asByteBuffer(), 4,
                (int) res.size - 4);
        return res.subSize(size + 4);
    }

    @Override
    public MemoryBuffer decompress(MemoryBuffer saveData) {
        if (saveData.size < 4) {
            me.cortex.voxy.common.Logger.warn("Failed to decompress section, data size too small: " + saveData.size);
            return null;
        }
        try {
            int decompressedSize = UnsafeUtil.memGetInt(saveData.address);
            if (decompressedSize < 0 || decompressedSize > (SaveLoadSystem.BIGGEST_SERIALIZED_SECTION_SIZE + 1024)) {
                me.cortex.voxy.common.Logger
                        .warn("Failed to decompress section, invalid decompressed size: " + decompressedSize
                                + " (raw bytes: " + formatFirstBytes(saveData) + ")");
                return null;
            }
            var res = SCRATCH.get().createUntrackedUnfreeableReference();
            int size = this.decompressor.decompress(saveData.asByteBuffer(), 4, res.asByteBuffer(), 0,
                    decompressedSize);
            return res.subSize(size);
        } catch (Exception e) {
            me.cortex.voxy.common.Logger.warn("Failed to decompress section (size=" + saveData.size
                    + ", first bytes: " + formatFirstBytes(saveData) + ")", e);
            return null;
        }
    }

    private static String formatFirstBytes(MemoryBuffer data) {
        StringBuilder sb = new StringBuilder();
        int len = (int) Math.min(32, data.size);
        for (int i = 0; i < len; i++) {
            if (i > 0)
                sb.append(" ");
            byte b = UnsafeUtil.memGetByte(data.address + i);
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }

    @Override
    public void close() {
    }

    public static class Config extends CompressorConfig {

        @Override
        public StorageCompressor build(ConfigBuildCtx ctx) {
            return new LZ4Compressor();
        }

        public static String getConfigTypeName() {
            return "LZ4";
        }
    }
}
