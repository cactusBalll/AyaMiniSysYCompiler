package util;

public class MyNode<E> {
    E value;
    MyNode<E> next;
    MyNode<E> prev;

    MyList<E> owner;

    public MyList<E> getOwner() {
        return owner;
    }

    public void setOwner(MyList<E> owner) {
        this.owner = owner;
    }

    public MyNode(MyNode<E> prev, E value, MyNode<E> next) {
        this.value = value;
        this.next = next;
        this.prev = prev;
    }

    public  MyNode(E value) {
        this.value = value;
        this.next = null;
        this.prev = null;
    }


    public void insertBefore(MyNode<E> other) {
        if (prev != null) {
            prev.next = other;
        } else {
            getOwner().setFirst(other);
        }
        other.prev = prev;
        prev = other;
        other.next = this;
        this.getOwner().increaseSize();
        other.setOwner(this.getOwner());
    }

    public void insertAfter(MyNode<E> other) {
        if (next != null) {
            next.prev = other;
        } else {
            getOwner().setLast(other);
        }
        other.next = next;
        next = other;
        other.prev = this;
        this.getOwner().increaseSize();
        other.setOwner(this.getOwner());
    }

    public void removeMe() {
        getOwner().remove(this);
    }

    public MyNode<E> getNext() {
        return next;
    }

    public MyNode<E> getPrev() {
        return prev;
    }

    public E getValue() {
        return value;
    }
}
