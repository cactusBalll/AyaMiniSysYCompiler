package exceptions;

public class BackEndErr extends Exception{
    @Override
    public String toString() {
        return "something wrong with backend.";
    }
}
