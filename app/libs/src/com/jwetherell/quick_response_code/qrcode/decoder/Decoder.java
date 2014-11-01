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

import java.util.Map;

import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.common.reedsolomon.GenericGF;
import com.google.zxing.common.reedsolomon.ReedSolomonDecoder;
import com.google.zxing.common.reedsolomon.ReedSolomonException;

/**
 * <p>
 * The main class which implements QR Code decoding -- as opposed to locating
 * and extracting the QR Code from an image.
 * </p>
 * 
 * @author Sean Owen
 */
public final class Decoder {

    private final com.google.zxing.common.reedsolomon.ReedSolomonDecoder rsDecoder;

    public Decoder() {
        rsDecoder = new com.google.zxing.common.reedsolomon.ReedSolomonDecoder(com.google.zxing.common.reedsolomon.GenericGF.QR_CODE_FIELD_256);
    }

    public com.google.zxing.common.DecoderResult decode(boolean[][] image) throws com.google.zxing.ChecksumException, com.google.zxing.FormatException {
        return decode(image, null);
    }

    /**
     * <p>
     * Convenience method that can decode a QR Code represented as a 2D array of
     * booleans. "true" is taken to mean a black module.
     * </p>
     * 
     * @param image
     *            booleans representing white/black QR Code modules
     * @return text and bytes encoded within the QR Code
     * @throws com.google.zxing.FormatException
     *             if the QR Code cannot be decoded
     * @throws com.google.zxing.ChecksumException
     *             if error correction fails
     */
    public com.google.zxing.common.DecoderResult decode(boolean[][] image, java.util.Map<com.google.zxing.DecodeHintType, ?> hints) throws com.google.zxing.ChecksumException, com.google.zxing.FormatException {
        int dimension = image.length;
        com.google.zxing.common.BitMatrix bits = new com.google.zxing.common.BitMatrix(dimension);
        for (int i = 0; i < dimension; i++) {
            for (int j = 0; j < dimension; j++) {
                if (image[i][j]) {
                    bits.set(j, i);
                }
            }
        }
        return decode(bits, hints);
    }

    public com.google.zxing.common.DecoderResult decode(com.google.zxing.common.BitMatrix bits) throws com.google.zxing.ChecksumException, com.google.zxing.FormatException {
        return decode(bits, null);
    }

    /**
     * <p>
     * Decodes a QR Code represented as a {@link com.google.zxing.common.BitMatrix}. A 1 or "true" is
     * taken to mean a black module.
     * </p>
     * 
     * @param bits
     *            booleans representing white/black QR Code modules
     * @return text and bytes encoded within the QR Code
     * @throws com.google.zxing.FormatException
     *             if the QR Code cannot be decoded
     * @throws com.google.zxing.ChecksumException
     *             if error correction fails
     */
    public com.google.zxing.common.DecoderResult decode(com.google.zxing.common.BitMatrix bits, java.util.Map<com.google.zxing.DecodeHintType, ?> hints) throws com.google.zxing.FormatException, com.google.zxing.ChecksumException {

        // Construct a parser and read version, error-correction level
        BitMatrixParser parser = new BitMatrixParser(bits);
        Version version = parser.readVersion();
        ErrorCorrectionLevel ecLevel = parser.readFormatInformation().getErrorCorrectionLevel();

        // Read codewords
        byte[] codewords = parser.readCodewords();
        // Separate into data blocks
        DataBlock[] dataBlocks = DataBlock.getDataBlocks(codewords, version, ecLevel);

        // Count total number of data bytes
        int totalBytes = 0;
        for (DataBlock dataBlock : dataBlocks) {
            totalBytes += dataBlock.getNumDataCodewords();
        }
        byte[] resultBytes = new byte[totalBytes];
        int resultOffset = 0;

        // Error-correct and copy data blocks together into a stream of bytes
        for (DataBlock dataBlock : dataBlocks) {
            byte[] codewordBytes = dataBlock.getCodewords();
            int numDataCodewords = dataBlock.getNumDataCodewords();
            correctErrors(codewordBytes, numDataCodewords);
            for (int i = 0; i < numDataCodewords; i++) {
                resultBytes[resultOffset++] = codewordBytes[i];
            }
        }

        // Decode the contents of that stream of bytes
        return DecodedBitStreamParser.decode(resultBytes, version, ecLevel, hints);
    }

    /**
     * <p>
     * Given data and error-correction codewords received, possibly corrupted by
     * errors, attempts to correct the errors in-place using Reed-Solomon error
     * correction.
     * </p>
     * 
     * @param codewordBytes
     *            data and error correction codewords
     * @param numDataCodewords
     *            number of codewords that are data bytes
     * @throws com.google.zxing.ChecksumException
     *             if error correction fails
     */
    private void correctErrors(byte[] codewordBytes, int numDataCodewords) throws com.google.zxing.ChecksumException {
        int numCodewords = codewordBytes.length;
        // First read into an array of ints
        int[] codewordsInts = new int[numCodewords];
        for (int i = 0; i < numCodewords; i++) {
            codewordsInts[i] = codewordBytes[i] & 0xFF;
        }
        int numECCodewords = codewordBytes.length - numDataCodewords;
        try {
            rsDecoder.decode(codewordsInts, numECCodewords);
        } catch (com.google.zxing.common.reedsolomon.ReedSolomonException rse) {
            throw com.google.zxing.ChecksumException.getChecksumInstance();
        }
        // Copy back into array of bytes -- only need to worry about the bytes
        // that were data
        // We don't care about errors in the error-correction codewords
        for (int i = 0; i < numDataCodewords; i++) {
            codewordBytes[i] = (byte) codewordsInts[i];
        }
    }

}
