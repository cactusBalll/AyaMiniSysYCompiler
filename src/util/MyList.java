package util;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.function.Consumer;

//封装自己的链表，方便插入

/**
 * 这个类来自比赛时的项目
 * @param <E>
 */
public class MyList<E> implements Iterable<MyNode<E>> {
    private int size = 0;
    private MyNode<E> first;
    private MyNode<E> last;

    /**
     * 浅复制
     * @return 复制的链表，但是node并不复制
     */
    @Override
    public MyList<E> clone() {
        MyList<E> ret = new MyList<E>();
        for (MyNode<E> node :
                this) {
            ret.addLast(node);
        }
        return ret;
    }
    public List<E> toList() {
        ArrayList<E> ret = new ArrayList<E>();
        for (MyNode<E> node :
                this) {
            ret.add(node.value);
        }
        return ret;
    }



    public int getSize() {
        return size;
    }

    public void setFirst(MyNode<E> first) {
        this.first = first;
    }

    public void setLast(MyNode<E> last) {
        this.last = last;
    }

    public void addLast(MyNode<E> node) {
        if (last != null) {
            last.next = node;
            node.prev = last;
            last = node;
        } else {
            last = node;
            first = node;
        }
        size++;
        node.setOwner(this);
    }

    public void add(MyNode<E> node) {
        addLast(node);
    }

    public void addFirst(MyNode<E> node) {
        if (first != null) {
            first.prev = node;
            node.next = first;
            first = node;
        } else {
            last = node;
            first = node;
        }
        size++;
        node.setOwner(this);
    }

    public MyList<E> removeAllAfter(MyNode<E> node) {
        MyList<E> ret = new MyList<>();
        MyNode<E> latterSegFirst = node.next;
        MyNode<E> latterSegLast = last;
        if (node.next != null) {
            node.next.prev = null;
        }
        node.next = null;
        last = node;
        this.size = this.recalculateSize();
        if (latterSegFirst != null) {
            ret.first = latterSegFirst;
            ret.last = latterSegLast;
            ret.size = ret.recalculateSize();
        }
        for (MyNode<E> retNode :
                ret) {
            retNode.setOwner(ret);
        }
        return ret;
    }

    public int recalculateSize() {
        int s = 0;
        MyNode<E> p = first;
        while (p != null) {
            s++;
            p = p.next;
        }
        return s;
    }
    /*
        注意该方法无法在迭代时删除节点
     */
    public void remove(MyNode<E> node) {
        // 检查node是否存在
        boolean flag = false;
        if (first == last && first == node) {
            flag = true;
            first = last = null;
        } else if (node == first) {
            flag = true;
            first = node.next;
            node.next.prev = null;
        } else if (node == last) {
            flag = true;
            last = node.prev;
            node.prev.next = null;
        } else {
            MyNode<E> curNode = first;
            while (curNode != null && curNode.next != null) {
                if (curNode.next == node) {
                    flag = true;
                    break;
                }
                curNode = curNode.next;
            }
            if (flag) {
                if (node.prev != null) {
                    node.prev.next = node.next;
                }
                if (node.next != null) {
                    node.next.prev = node.prev;
                }
            }
        }
        node.prev = node.next = null;

        if (flag) {
            size--;
        }
    }
    public MyNode<E> getFirst() {
        return first;
    }

    public MyNode<E> getLast() {
        return last;
    }

    public void replace(MyNode<E> oldNode, MyNode<E> newNode) {
        newNode.next = oldNode.next;
        newNode.prev = oldNode.prev;
        if (first == oldNode) {
            first = newNode;
        }
        if (last == oldNode) {
            last = newNode;
        }
        if (oldNode.prev != null) {
            oldNode.prev.next = newNode;
        }
        if (oldNode.next != null) {
            oldNode.next.prev = newNode;
        }
        oldNode.prev = oldNode.next = null;
        newNode.setOwner(this);
    }

    class MyIterator implements Iterator<MyNode<E>> {
        private int index = 0;
        private MyNode<E> curent = first;
        private MyNode<E> next = first;
        @Override
        public void remove() {
            index--;
            MyNode<E> temp = curent.next;
            curent.removeMe();
        }

        @Override
        public void forEachRemaining(Consumer<? super MyNode<E>> action) {
            Iterator.super.forEachRemaining(action);
        }

        @Override
        public boolean hasNext() {
            return index < size;
        }

        @Override
        public MyNode<E> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            curent = next;
            if (next == null) {
                System.out.println();
            }
            next = next.next;
            index++;
            return curent;
        }
    }

    public void decreaseSize() {
        assert size > 0;
        size--;
    }

    public void increaseSize() {
        size++;
    }
    @Override
    public Spliterator spliterator() {
        return Iterable.super.spliterator();
    }

    @Override
    public Iterator<MyNode<E>> iterator() {
        return new MyIterator();
    }
}

