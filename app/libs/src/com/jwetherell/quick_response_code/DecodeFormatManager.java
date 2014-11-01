/*
 * Copyright (C) 2010 ZXing authors
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

package com.jwetherell.quick_response_code;

import java.util.Collection;
import java.util.EnumSet;

import com.google.zxing.BarcodeFormat;

final class DecodeFormatManager {

    private DecodeFormatManager() {
    };

    static final java.util.Collection<com.google.zxing.BarcodeFormat> PRODUCT_FORMATS;
    static final java.util.Collection<com.google.zxing.BarcodeFormat> ONE_D_FORMATS;
    static final java.util.Collection<com.google.zxing.BarcodeFormat> QR_CODE_FORMATS = java.util.EnumSet.of(com.google.zxing.BarcodeFormat.QR_CODE);
    static final java.util.Collection<com.google.zxing.BarcodeFormat> DATA_MATRIX_FORMATS = java.util.EnumSet.of(com.google.zxing.BarcodeFormat.DATA_MATRIX);
    static {
        PRODUCT_FORMATS = java.util.EnumSet.of(com.google.zxing.BarcodeFormat.UPC_A, com.google.zxing.BarcodeFormat.UPC_E, com.google.zxing.BarcodeFormat.EAN_13, com.google.zxing.BarcodeFormat.EAN_8, com.google.zxing.BarcodeFormat.RSS_14);
        ONE_D_FORMATS = java.util.EnumSet.of(com.google.zxing.BarcodeFormat.CODE_39, com.google.zxing.BarcodeFormat.CODE_93, com.google.zxing.BarcodeFormat.CODE_128, com.google.zxing.BarcodeFormat.ITF);
        ONE_D_FORMATS.addAll(PRODUCT_FORMATS);
    }
}
