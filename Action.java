package org.example;

import java.io.Serializable;

public class Action implements Serializable {
    public enum Type { PLACE, KILL }
    public final Type type;
    public final int row; // 0..9
    public final int col; // 0..9

    public Action(Type type, int row, int col) {
        this.type = type;
        this.row = row;
        this.col = col;
    }
}
