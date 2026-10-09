package com.murthinext.ae2pr.client.proto_terminal;

import java.util.ArrayList;
import java.util.List;

/** 按帧缓冲像素生成完整平行四边形的裁剪带。 */
final class ParallelogramClip {

    record Band(int left, int top, int right, int bottom) {
    }

    private ParallelogramClip() {
    }

    static List<Band> bands(double x, double y, double width, double height, double slant) {
        if (width <= slant || height <= 0) {
            return List.of();
        }
        List<Band> bands = new ArrayList<>();
        int top = (int) Math.ceil(y - 0.5);
        int bottom = (int) Math.ceil(y + height - 0.5);
        for (int row = top; row < bottom; row++) {
            double edge = x + slant * (1 - (row + 0.5 - y) / height);
            int left = (int) Math.ceil(edge - 0.5);
            int right = (int) Math.ceil(edge + width - slant - 0.5);
            if (right <= left) {
                continue;
            }
            if (!bands.isEmpty()) {
                Band previous = bands.get(bands.size() - 1);
                if (previous.left() == left && previous.right() == right && previous.bottom() == row) {
                    bands.set(bands.size() - 1, new Band(left, previous.top(), right, row + 1));
                    continue;
                }
            }
            bands.add(new Band(left, row, right, row + 1));
        }
        return List.copyOf(bands);
    }
}
