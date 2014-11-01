/*
 * Copyright 2007 ZXing authors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jwetherell.quick_response_code.qrcode.decoder;

import com.google.zxing.FormatException;
import com.google.zxing.common.BitMatrix;

/**
 * See ISO 18004:2006 Annex D
 * 
 * @author Sean Owen
 */
public final class Version {

    /**
     * See ISO 18004:2006 Annex D. Element i represents the raw version bits
     * that specify version i + 7
     */
    private static final int[] VERSION_DECODE_INFO = { 0x07C94, 0x085BC, 0x09A99, 0x0A4D3, 0x0BBF6, 0x0C762, 0x0D847, 0x0E60D, 0x0F928, 0x10B78, 0x1145D,
            0x12A17, 0x13532, 0x149A6, 0x15683, 0x168C9, 0x177EC, 0x18EC4, 0x191E1, 0x1AFAB, 0x1B08E, 0x1CC1A, 0x1D33F, 0x1ED75, 0x1F250, 0x209D5, 0x216F0,
            0x228BA, 0x2379F, 0x24B0B, 0x2542E, 0x26A64, 0x27541, 0x28C69 };

    private static final com.jwetherell.quick_response_code.qrcode.decoder.Version[] VERSIONS = buildVersions();

    private final int versionNumber;
    private final int[] alignmentPatternCenters;
    private final com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks[] ecBlocks;
    private final int totalCodewords;

    private Version(int versionNumber, int[] alignmentPatternCenters, com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks... ecBlocks) {
        this.versionNumber = versionNumber;
        this.alignmentPatternCenters = alignmentPatternCenters;
        this.ecBlocks = ecBlocks;
        int total = 0;
        int ecCodewords = ecBlocks[0].getECCodewordsPerBlock();
        com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB[] ecbArray = ecBlocks[0].getECBlocks();
        for (com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB ecBlock : ecbArray) {
            total += ecBlock.getCount() * (ecBlock.getDataCodewords() + ecCodewords);
        }
        this.totalCodewords = total;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public int[] getAlignmentPatternCenters() {
        return alignmentPatternCenters;
    }

    public int getTotalCodewords() {
        return totalCodewords;
    }

    public int getDimensionForVersion() {
        return 17 + 4 * versionNumber;
    }

    public com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks getECBlocksForLevel(ErrorCorrectionLevel ecLevel) {
        return ecBlocks[ecLevel.ordinal()];
    }

    /**
     * <p>
     * Deduces version information purely from QR Code dimensions.
     * </p>
     * 
     * @param dimension
     *            dimension in modules
     * @return Version for a QR Code of that dimension
     * @throws com.google.zxing.FormatException
     *             if dimension is not 1 mod 4
     */
    public static com.jwetherell.quick_response_code.qrcode.decoder.Version getProvisionalVersionForDimension(int dimension) throws com.google.zxing.FormatException {
        if (dimension % 4 != 1) {
            throw com.google.zxing.FormatException.getFormatInstance();
        }
        try {
            return getVersionForNumber((dimension - 17) >> 2);
        } catch (IllegalArgumentException iae) {
            throw com.google.zxing.FormatException.getFormatInstance();
        }
    }

    public static com.jwetherell.quick_response_code.qrcode.decoder.Version getVersionForNumber(int versionNumber) {
        if (versionNumber < 1 || versionNumber > 40) {
            throw new IllegalArgumentException();
        }
        return VERSIONS[versionNumber - 1];
    }

    static com.jwetherell.quick_response_code.qrcode.decoder.Version decodeVersionInformation(int versionBits) {
        int bestDifference = Integer.MAX_VALUE;
        int bestVersion = 0;
        for (int i = 0; i < VERSION_DECODE_INFO.length; i++) {
            int targetVersion = VERSION_DECODE_INFO[i];
            // Do the version info bits match exactly? done.
            if (targetVersion == versionBits) {
                return getVersionForNumber(i + 7);
            }
            // Otherwise see if this is the closest to a real version info bit
            // string
            // we have seen so far
            int bitsDifference = FormatInformation.numBitsDiffering(versionBits, targetVersion);
            if (bitsDifference < bestDifference) {
                bestVersion = i + 7;
                bestDifference = bitsDifference;
            }
        }
        // We can tolerate up to 3 bits of error since no two version info
        // codewords will
        // differ in less than 8 bits.
        if (bestDifference <= 3) {
            return getVersionForNumber(bestVersion);
        }
        // If we didn't find a close enough match, fail
        return null;
    }

    /**
     * See ISO 18004:2006 Annex E
     */
    com.google.zxing.common.BitMatrix buildFunctionPattern() {
        int dimension = getDimensionForVersion();
        com.google.zxing.common.BitMatrix bitMatrix = new com.google.zxing.common.BitMatrix(dimension);

        // Top left finder pattern + separator + format
        bitMatrix.setRegion(0, 0, 9, 9);
        // Top right finder pattern + separator + format
        bitMatrix.setRegion(dimension - 8, 0, 8, 9);
        // Bottom left finder pattern + separator + format
        bitMatrix.setRegion(0, dimension - 8, 9, 8);

        // Alignment patterns
        int max = alignmentPatternCenters.length;
        for (int x = 0; x < max; x++) {
            int i = alignmentPatternCenters[x] - 2;
            for (int y = 0; y < max; y++) {
                if ((x == 0 && (y == 0 || y == max - 1)) || (x == max - 1 && y == 0)) {
                    // No alignment patterns near the three finder paterns
                    continue;
                }
                bitMatrix.setRegion(alignmentPatternCenters[y] - 2, i, 5, 5);
            }
        }

        // Vertical timing pattern
        bitMatrix.setRegion(6, 9, 1, dimension - 17);
        // Horizontal timing pattern
        bitMatrix.setRegion(9, 6, dimension - 17, 1);

        if (versionNumber > 6) {
            // Version info, top right
            bitMatrix.setRegion(dimension - 11, 0, 3, 6);
            // Version info, bottom left
            bitMatrix.setRegion(0, dimension - 11, 6, 3);
        }

        return bitMatrix;
    }

    /**
     * <p>
     * Encapsulates a set of error-correction blocks in one symbol version. Most
     * versions will use blocks of differing sizes within one version, so, this
     * encapsulates the parameters for each set of blocks. It also holds the
     * number of error-correction codewords per block since it will be the same
     * across all blocks within one version.
     * </p>
     */
    public static final class ECBlocks {

        private final int ecCodewordsPerBlock;
        private final com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB[] ecBlocks;

        ECBlocks(int ecCodewordsPerBlock, com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB... ecBlocks) {
            this.ecCodewordsPerBlock = ecCodewordsPerBlock;
            this.ecBlocks = ecBlocks;
        }

        public int getECCodewordsPerBlock() {
            return ecCodewordsPerBlock;
        }

        public int getNumBlocks() {
            int total = 0;
            for (com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB ecBlock : ecBlocks) {
                total += ecBlock.getCount();
            }
            return total;
        }

        public int getTotalECCodewords() {
            return ecCodewordsPerBlock * getNumBlocks();
        }

        public com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB[] getECBlocks() {
            return ecBlocks;
        }
    }

    /**
     * <p>
     * Encapsualtes the parameters for one error-correction block in one symbol
     * version. This includes the number of data codewords, and the number of
     * times a block with these parameters is used consecutively in the QR code
     * version's format.
     * </p>
     */
    public static final class ECB {

        private final int count;
        private final int dataCodewords;

        ECB(int count, int dataCodewords) {
            this.count = count;
            this.dataCodewords = dataCodewords;
        }

        public int getCount() {
            return count;
        }

        public int getDataCodewords() {
            return dataCodewords;
        }
    }

    @Override
    public String toString() {
        return String.valueOf(versionNumber);
    }

    /**
     * See ISO 18004:2006 6.5.1 Table 9
     */
    private static com.jwetherell.quick_response_code.qrcode.decoder.Version[] buildVersions() {
        return new com.jwetherell.quick_response_code.qrcode.decoder.Version[] {
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(1, new int[] {}, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(7, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(1, 19)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(10, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(1, 16)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(13, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(1, 13)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(
                        17, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(1, 9))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(2, new int[] { 6, 18 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(10, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(1, 34)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(16, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(1, 28)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(22, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(1, 22)),
                        new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(1, 16))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(3, new int[] { 6, 22 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(15, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(1, 55)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(26, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(1, 44)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(18, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(2, 17)),
                        new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(22, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(2, 13))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(4, new int[] { 6, 26 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(20, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(1, 80)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(18, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(2, 32)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(26, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(2, 24)),
                        new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(16, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 9))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(5, new int[] { 6, 30 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(26, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(1, 108)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(24, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(2, 43)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(18, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(2, 15),
                        new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(2, 16)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(22, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(2, 11), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(2, 12))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(6, new int[] { 6, 34 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(18, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(2, 68)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(16, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 27)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(24, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 19)),
                        new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 15))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(7, new int[] { 6, 22, 38 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(20, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(2, 78)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(18, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 31)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(18, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(2, 14),
                        new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 15)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(26, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 13), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(1, 14))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(8, new int[] { 6, 24, 42 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(24, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(2, 97)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(22, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(2, 38), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(2, 39)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(22,
                        new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 18), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(2, 19)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(26, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 14), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(2, 15))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(9, new int[] { 6, 26, 46 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(2, 116)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(22, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(3, 36), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(2, 37)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(20,
                        new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 16), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 17)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(24, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 12), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 13))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(10, new int[] { 6, 28, 50 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(18, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(2, 68), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(2, 69)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(26, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 43), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(1, 44)),
                        new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(24, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(6, 19), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(2, 20)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(6, 15), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(2, 16))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(11, new int[] { 6, 30, 54 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(20, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 81)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(1, 50), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 51)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28,
                        new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 22), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 23)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(24, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(3, 12), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(8, 13))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(12, new int[] { 6, 32, 58 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(24, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(2, 92), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(2, 93)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(22, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(6, 36), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(2, 37)),
                        new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(26, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 20), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(6, 21)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(7, 14), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 15))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(13, new int[] { 6, 34, 62 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(26, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 107)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(22, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(8, 37), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(1, 38)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(24,
                        new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(8, 20), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 21)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(22, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(12, 11), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 12))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(14, new int[] { 6, 26, 46, 66 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(3, 115), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(1, 116)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(24, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 40),
                        new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(5, 41)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(20, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(11, 16), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(5, 17)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(24, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(11, 12), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(5, 13))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(15, new int[] { 6, 26, 48, 70 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(22, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(5, 87), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(1, 88)),
                        new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(24, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(5, 41), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(5, 42)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(5, 24), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(7, 25)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(24, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(11, 12),
                                new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(7, 13))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(16, new int[] { 6, 26, 50, 74 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(24, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(5, 98), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(1, 99)),
                        new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(7, 45), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(3, 46)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(24, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(15, 19), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(2, 20)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(3, 15),
                                new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(13, 16))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(17, new int[] { 6, 30, 54, 78 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(1, 107), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(5, 108)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(10, 46), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(1,
                        47)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(1, 22), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(15, 23)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(2, 14), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(17, 15))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(18, new int[] { 6, 30, 56, 82 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(5, 120), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(1, 121)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(26, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(9, 43),
                        new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 44)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(17, 22), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(1, 23)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(2, 14), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(19, 15))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(19, new int[] { 6, 30, 58, 86 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(3, 113), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 114)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(26, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(3, 44), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(11,
                        45)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(26, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(17, 21), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 22)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(26, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(9, 13), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(16, 14))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(20, new int[] { 6, 34, 62, 90 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(3, 107), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(5, 108)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(26, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(3, 41), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(13,
                        42)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(15, 24), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(5, 25)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(15, 15), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(10, 16))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(21, new int[] { 6, 28, 50, 72, 94 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 116), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 117)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(26, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(17, 42)),
                        new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(17, 22), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(6, 23)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(19, 16), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(6, 17))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(22, new int[] { 6, 26, 50, 74, 98 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(2, 111), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(7, 112)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(17, 46)),
                        new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(7, 24), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(16, 25)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(24, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(34, 13))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(23, new int[] { 6, 30, 54, 78, 102 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 121), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(5, 122)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 47), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(
                        14, 48)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(11, 24), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(14, 25)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(16, 15), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(14, 16))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(24, new int[] { 6, 28, 54, 80, 106 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(6, 117), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 118)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(6, 45), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(
                        14, 46)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(11, 24), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(16, 25)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(30, 16), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(2, 17))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(25, new int[] { 6, 32, 58, 84, 110 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(26, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(8, 106), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 107)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(8, 47), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(
                        13, 48)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(7, 24), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(22, 25)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(22, 15), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(13, 16))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(26, new int[] { 6, 30, 58, 86, 114 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(10, 114), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(2, 115)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(19, 46),
                        new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 47)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(28, 22), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(6, 23)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(33, 16), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 17))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(27, new int[] { 6, 34, 62, 90, 118 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(8, 122), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 123)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(22, 45),
                        new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(3, 46)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(8, 23), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(26, 24)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(12, 15), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(28, 16))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(28, new int[] { 6, 26, 50, 74, 98, 122 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(3, 117), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(10, 118)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(3, 45),
                        new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(23, 46)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 24), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(31, 25)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(11, 15), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(31, 16))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(29, new int[] { 6, 30, 54, 78, 102, 126 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(7, 116), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(7, 117)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(21, 45),
                        new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(7, 46)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(1, 23), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(37, 24)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(19, 15), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(26, 16))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(30, new int[] { 6, 26, 52, 78, 104, 130 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(5, 115), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(10, 116)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(19, 47),
                        new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(10, 48)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(15, 24), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(25, 25)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(23, 15), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(25, 16))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(31, new int[] { 6, 30, 56, 82, 108, 134 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(13, 115), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(3, 116)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(2, 46),
                        new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(29, 47)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(42, 24), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(1, 25)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(23, 15), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(28, 16))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(32, new int[] { 6, 34, 60, 86, 112, 138 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(17, 115)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(10, 46), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(23, 47)),
                        new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(10, 24), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(35, 25)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(19, 15), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(35, 16))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(33, new int[] { 6, 30, 58, 86, 114, 142 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(17, 115), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(1, 116)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(14, 46),
                        new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(21, 47)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(29, 24), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(19, 25)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(11, 15), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(46, 16))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(34, new int[] { 6, 34, 62, 90, 118, 146 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(13, 115), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(6, 116)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(14, 46),
                        new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(23, 47)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(44, 24), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(7, 25)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(59, 16), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(1, 17))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(35, new int[] { 6, 30, 54, 78, 102, 126, 150 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(12, 121), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(7, 122)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(12,
                        47), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(26, 48)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(39, 24), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(14, 25)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(22, 15), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(41, 16))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(36, new int[] { 6, 24, 50, 76, 102, 128, 154 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(6, 121), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(14, 122)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28,
                        new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(6, 47), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(34, 48)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(46, 24), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(10, 25)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(2, 15), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(64, 16))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(37, new int[] { 6, 28, 54, 80, 106, 132, 158 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(17, 122), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 123)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(29,
                        46), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(14, 47)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(49, 24), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(10, 25)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(24, 15), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(46, 16))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(38, new int[] { 6, 32, 58, 84, 110, 136, 162 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 122), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(18, 123)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(13,
                        46), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(32, 47)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(48, 24), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(14, 25)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(42, 15), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(32, 16))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(39, new int[] { 6, 26, 54, 82, 110, 138, 166 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(20, 117), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(4, 118)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(40,
                        47), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(7, 48)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(43, 24), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(22, 25)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(10, 15), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(67, 16))),
                new com.jwetherell.quick_response_code.qrcode.decoder.Version(40, new int[] { 6, 30, 58, 86, 114, 142, 170 }, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(19, 118), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(6, 119)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(28, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(18,
                        47), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(31, 48)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(34, 24), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(34, 25)), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECBlocks(30, new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(20, 15), new com.jwetherell.quick_response_code.qrcode.decoder.Version.ECB(61, 16))) };
    }

}
