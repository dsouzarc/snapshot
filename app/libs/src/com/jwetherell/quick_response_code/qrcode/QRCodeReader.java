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

package com.jwetherell.quick_response_code.qrcode;

import java.util.List;
import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.common.DetectorResult;
import com.jwetherell.quick_response_code.qrcode.decoder.Decoder;
import com.jwetherell.quick_response_code.qrcode.detector.Detector;

/**
 * This implementation can detect and decode QR Codes in an image.
 * 
 * @author Sean Owen
 */
public class QRCodeReader implements com.google.zxing.Reader {

    private static final com.google.zxing.ResultPoint[] NO_POINTS = new com.google.zxing.ResultPoint[0];

    private final Decoder decoder = new Decoder();

    protected Decoder getDecoder() {
        return decoder;
    }

    /**
     * Locates and decodes a QR code in an image.
     * 
     * @return a String representing the content encoded by the QR code
     * @throws com.google.zxing.NotFoundException
     *             if a QR code cannot be found
     * @throws com.google.zxing.FormatException
     *             if a QR code cannot be decoded
     * @throws com.google.zxing.ChecksumException
     *             if error correction fails
     */
    @Override
    public com.google.zxing.Result decode(com.google.zxing.BinaryBitmap image) throws com.google.zxing.NotFoundException, com.google.zxing.ChecksumException, com.google.zxing.FormatException {
        return decode(image, null);
    }

    @Override
    public com.google.zxing.Result decode(com.google.zxing.BinaryBitmap image, java.util.Map<com.google.zxing.DecodeHintType, ?> hints) throws com.google.zxing.NotFoundException, com.google.zxing.ChecksumException, com.google.zxing.FormatException {
        com.google.zxing.common.DecoderResult decoderResult;
        com.google.zxing.ResultPoint[] points;
        if (hints != null && hints.containsKey(com.google.zxing.DecodeHintType.PURE_BARCODE)) {
            com.google.zxing.common.BitMatrix bits = extractPureBits(image.getBlackMatrix());
            decoderResult = decoder.decode(bits, hints);
            points = NO_POINTS;
        } else {
            com.google.zxing.common.DetectorResult detectorResult = new Detector(image.getBlackMatrix()).detect(hints);
            decoderResult = decoder.decode(detectorResult.getBits(), hints);
            points = detectorResult.getPoints();
        }

        com.google.zxing.Result result = new com.google.zxing.Result(decoderResult.getText(), decoderResult.getRawBytes(), points, com.google.zxing.BarcodeFormat.QR_CODE);
        java.util.List<byte[]> byteSegments = decoderResult.getByteSegments();
        if (byteSegments != null) {
            result.putMetadata(com.google.zxing.ResultMetadataType.BYTE_SEGMENTS, byteSegments);
        }
        String ecLevel = decoderResult.getECLevel();
        if (ecLevel != null) {
            result.putMetadata(com.google.zxing.ResultMetadataType.ERROR_CORRECTION_LEVEL, ecLevel);
        }
        return result;
    }

    @Override
    public void reset() {
        // do nothing
    }

    /**
     * This method detects a code in a "pure" image -- that is, pure monochrome
     * image which contains only an unrotated, unskewed, image of a code, with
     * some white border around it. This is a specialized method that works
     * exceptionally fast in this special case.
     * 
     * @see com.google.zxing.pdf417.PDF417Reader#extractPureBits(com.google.zxing.common.BitMatrix)
     * @see com.google.zxing.datamatrix.DataMatrixReader#extractPureBits(com.google.zxing.common.BitMatrix)
     */
    private static com.google.zxing.common.BitMatrix extractPureBits(com.google.zxing.common.BitMatrix image) throws com.google.zxing.NotFoundException {

        int[] leftTopBlack = image.getTopLeftOnBit();
        int[] rightBottomBlack = image.getBottomRightOnBit();
        if (leftTopBlack == null || rightBottomBlack == null) {
            throw com.google.zxing.NotFoundException.getNotFoundInstance();
        }

        int moduleSize = moduleSize(leftTopBlack, image);

        int top = leftTopBlack[1];
        int bottom = rightBottomBlack[1];
        int left = leftTopBlack[0];
        int right = rightBottomBlack[0];

        if (bottom - top != right - left) {
            // Special case, where bottom-right module wasn't black so we found
            // something else in
            // the last row
            // Assume it's a square, so use height as the width
            right = left + (bottom - top);
        }

        int matrixWidth = (right - left + 1) / moduleSize;
        int matrixHeight = (bottom - top + 1) / moduleSize;
        if (matrixWidth <= 0 || matrixHeight <= 0) {
            throw com.google.zxing.NotFoundException.getNotFoundInstance();
        }
        if (matrixHeight != matrixWidth) {
            // Only possibly decode square regions
            throw com.google.zxing.NotFoundException.getNotFoundInstance();
        }

        // Push in the "border" by half the module width so that we start
        // sampling in the middle of the module. Just in case the image is a
        // little off, this will help recover.
        int nudge = moduleSize >> 1;
        top += nudge;
        left += nudge;

        // Now just read off the bits
        com.google.zxing.common.BitMatrix bits = new com.google.zxing.common.BitMatrix(matrixWidth, matrixHeight);
        for (int y = 0; y < matrixHeight; y++) {
            int iOffset = top + y * moduleSize;
            for (int x = 0; x < matrixWidth; x++) {
                if (image.get(left + x * moduleSize, iOffset)) {
                    bits.set(x, y);
                }
            }
        }
        return bits;
    }

    private static int moduleSize(int[] leftTopBlack, com.google.zxing.common.BitMatrix image) throws com.google.zxing.NotFoundException {
        int height = image.getHeight();
        int width = image.getWidth();
        int x = leftTopBlack[0];
        int y = leftTopBlack[1];
        while (x < width && y < height && image.get(x, y)) {
            x++;
            y++;
        }
        if (x == width || y == height) {
            throw com.google.zxing.NotFoundException.getNotFoundInstance();
        }

        int moduleSize = x - leftTopBlack[0];
        if (moduleSize == 0) {
            throw com.google.zxing.NotFoundException.getNotFoundInstance();
        }
        return moduleSize;
    }

}
