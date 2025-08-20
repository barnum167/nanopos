package com.elixirpay.elixirpaycat.helper;

import android.text.TextUtils;

import com.elixirpay.elixirpaycat.helper.command.Align;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EpsonPrinterHelper {
    private int mPageWidth = 42;

    private static EpsonPrinterHelper mInstance;

    public static synchronized EpsonPrinterHelper getInstance() {
        if(mInstance == null) {
            mInstance = new EpsonPrinterHelper();
        }

        return mInstance;
    }

    public EpsonPrinterHelper setPageWidth(int width) {
        mPageWidth = width;

        return this;
    }

    private List<Byte> getHeaderForm(String title) {
        List<Byte> list = new ArrayList<>();

        list.addAll(cmdAlign(Align.CENTER));
        list.addAll(cmdBatch(false, true, false, true));
        list.addAll(cmdString(title));
        list.addAll(cmdLF());
        list.addAll(cmdLF());

        return list;
    }

    private List<Byte> getFooterForm(String message) {
        List<Byte> list = new ArrayList<>();

        list.addAll(cmdString(message));
        list.addAll(cmdLF());
        list.addAll(cmdLF());
        list.addAll(cmdLF());
        list.addAll(cmdLF());

        return list;
    }

    private List<Byte> cmdInitialize() {
        return Arrays.asList(new Byte[] {0x1B, 0x40});
    }

    private List<Byte> cmdLanguage() {
        return Arrays.asList(new Byte[] { 0x1B, 0x59, 0x48, 0x43, 0x01});
    }

    private List<Byte> cmdPage() {
        return Arrays.asList(new Byte[] {0x1B, 0x74, (byte)0xFF});
    }

    private List<Byte> cmdLF() {
        return Arrays.asList(new Byte[]{0x0A});
    }

    private List<Byte> cmdAlign(Align align) {
        return Arrays.asList(new Byte[]{0x1B, 0x61, align.value()});
    }

    private List<Byte> cmdBatch(boolean font, boolean emphasize, boolean doubleTall, boolean doubleWide) {
        byte val = 0x00;
        val |= font ? 0x01 : 0x00;
        val |= emphasize ? 0x08 : 0x00;
        val |= doubleTall ? 0x10 : 0x00;
        val |= doubleWide ? 0x20 : 0x00;

        return Arrays.asList(new Byte[]{0x1B, 0x21, val});
    }

    private List<Byte> cmdPaperCut(boolean clockwise) {
        return Arrays.asList(new Byte[]{0x1D, 0x56, 0x42, clockwise ? (byte)0x01 : (byte)0x00});
    }

    private List<Byte> cmdLine() {
        byte[] lineArray = new byte[mPageWidth+1];

        int i = 0;

        for(; i<mPageWidth; i++) {
            lineArray[i] = '-';
        }

        lineArray[i] = 0x0A;

        return asList(lineArray);
    }

    private List<Byte> cmdString(String str) {
        byte[] strArray;

        try {
            strArray = str.getBytes("euc-kr");

        } catch (Exception e) {
            strArray = new byte[] {};
        }

        return asList(strArray);
    }

    private List<Byte> cmdStringLine(String left, String right) {
        ArrayList<Byte> list = new ArrayList<>();

        List<Byte> leftList = cmdString(left);
        List<Byte> rightList = cmdString(right);

        list.addAll(leftList);

        int space = mPageWidth - leftList.size() - rightList.size();
        if(space > 0) {
            list.addAll(cmdRepeatChar(' ', space));
        }

        list.addAll(rightList);
        list.addAll(cmdLF());

        return list;
    }

    private List<Byte> cmdAddress(String title, String body) {
        ArrayList<Byte> list = new ArrayList<>();

        List<Byte> titleList = cmdString(title);
        List<Byte> bodyList = cmdString(body);

        list.addAll(titleList);
        list.addAll(cmdString(" "));
        list.addAll(bodyList);
        list.addAll(cmdLF());

        return list;
    }

    private List<Byte> cmdAddress(String title, String body, String subBody) {
        ArrayList<Byte> list = new ArrayList<>();

        List<Byte> titleList = cmdString(title);
        List<Byte> bodyList = cmdString(body);
        List<Byte> subBodyList = cmdString(subBody);

        list.addAll(titleList);
        list.addAll(cmdString(" "));
        list.addAll(bodyList);
        list.addAll(cmdString(" "));
        list.addAll(subBodyList);
        list.addAll(cmdLF());

        return list;
    }

    private List<Byte> cmdRepeatChar(char c, int repeat) {
        ArrayList<Byte> list = new ArrayList<>();
        byte[] repeatArray = new byte[repeat];

        for(int i=0; i<repeat; i++) {
            repeatArray[i] = (byte)c;
        }

        list.addAll(asList(repeatArray));

        return list;
    }

    private List<Byte> asList(byte[] src) {
        Byte[] tempArray = new Byte[src.length];

        for(int i=0; i<tempArray.length; i++) {
            tempArray[i] = src[i];
        }

        return Arrays.asList(tempArray);
    }

    private byte[] getPrintCommand(ArrayList<Byte> commandList) {
        int size = commandList.size();
        byte[] commandArray = new byte[size];

        for(int i=0; i<size; i++) {
            commandArray[i] = commandList.get(i);
        }

        return commandArray;
    }

    private static boolean isEmptyAll(String... strings) {
        for(String str : strings) {
            if(!TextUtils.isEmpty(str)) {
                return false;
            }
        }

        return true;
    }
}
