
package main.optimizer;

public enum InstructionType {
    NEG, ADD,
    SUB, MUL, DIV,
    CMP,
    ADDA, LOAD, STORE,
    MOVE,
    PHI,
    END,
    BRA, BNE, BEQ, BLE, BLT, BGE, BGT,
    RETURN,
    LOADADD, STOREADD,
    READ, WRITE, WLN;

    public static String getInstructionName(InstructionType type) {
        switch (type) {
            case NEG:
                return "NEG";
            case ADD:
                return "ADD";
            case SUB:
                return "SUB";
            case MUL:
                return "MUL";
            case DIV:
                return "DIV";
            case CMP:
                return "CMP";
            case ADDA:
                return "ADDA";
            case LOAD:
                return "LOAD";
            case STORE:
                return "STORE";
            case MOVE:
                return "MOVE";
            case PHI:
                return "PHI";
            case END:
                return "END";
            case BRA:
                return "BRA";
            case BNE:
                return "BNE";
            case BEQ:
                return "BEQ";
            case BLE:
                return "BLE";
            case BLT:
                return "BLT";
            case BGE:
                return "BGE";
            case BGT:
                return "BGT";
            case RETURN:
                return "RETURN";
            case LOADADD:
                return "LOADADD";
            case STOREADD:
                return "STOREADD";
            case READ:
                return "READ";
            case WRITE:
                return "WRITE";
            case WLN:
                return "WLN";
            default:
                return null;
        }
    }

}
