package frontend;

import java.util.ArrayList;
import java.util.List;

public class ErrorHandler {
    private final static ErrorHandler instance = new ErrorHandler();
    private List<RequiredErr> requiredErrList = new ArrayList<>();

    public static ErrorHandler getInstance() {
        return instance;
    }

    public void addError(RequiredErr err) {
        requiredErrList.add(err);
    }

    public void clear() {
        requiredErrList.clear();
    }

    public boolean compileError() {
        return !requiredErrList.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (RequiredErr err :
                requiredErrList) {
            sb.append(err.toString()).append('\n');
        }
        return sb.toString();
    }
}
