#include <iostream>
#include <unordered_map>
#include <string>
#include <mutex>

using namespace std;

template<typename K, typename V>
class Node {
public:
    K key;
    V value;
    Node<K, V>* prev;
    Node<K, V>* next;

    Node(K key, V value) : key(key), value(value), prev(nullptr), next(nullptr) {}
};

template<typename K, typename V>
class DoublyLinkedList {
private:
    Node<K, V>* head;
    Node<K, V>* tail;

public:
    DoublyLinkedList() {
        head = new Node<K, V>(K{}, V{});
        tail = new Node<K, V>(K{}, V{});
        head->next = tail;
        tail->prev = head;
    }

    void addFirst(Node<K, V>* node) {
        node->next = head->next;
        node->prev = head;
        head->next->prev = node;
        head->next = node;
    }

    void remove(Node<K, V>* node) {
        node->prev->next = node->next;
        node->next->prev = node->prev;
    }

    void moveToFront(Node<K, V>* node) {
        remove(node);
        addFirst(node);
    }

    Node<K, V>* removeLast() {
        if (tail->prev == head) return nullptr;
        Node<K, V>* last = tail->prev;
        remove(last);
        return last;
    }
};


template<typename K, typename V>
class LRUCache {
private:
    int capacity;
    unordered_map<K, Node<K, V>*> map;
    DoublyLinkedList<K, V>* dll;
    mutex mtx;

public:
    LRUCache(int capacity) : capacity(capacity) {
        dll = new DoublyLinkedList<K, V>();
    }

    V get(K key) {
        lock_guard<mutex> lock(mtx);
        if (map.find(key) == map.end()) return V{};
        Node<K, V>* node = map[key];
        dll->moveToFront(node);
        return node->value;
    }

    void put(K key, V value) {
        lock_guard<mutex> lock(mtx);
        if (map.find(key) != map.end()) {
            Node<K, V>* node = map[key];
            node->value = value;
            dll->moveToFront(node);
        } else {
            if (map.size() == capacity) {
                Node<K, V>* lru = dll->removeLast();
                if (lru != nullptr) {
                    map.erase(lru->key);
                    delete lru;
                }
            }
            Node<K, V>* newNode = new Node<K, V>(key, value);
            dll->addFirst(newNode);
            map[key] = newNode;
        }
    }

    void remove(K key) {
        lock_guard<mutex> lock(mtx);
        if (map.find(key) == map.end()) return;
        Node<K, V>* node = map[key];
        dll->remove(node);
        map.erase(key);
        delete node;
    }
};


class LRUCacheDemo {
public:
    static void main() {
        LRUCache<string, int>* cache = new LRUCache<string, int>(3);

        cache->put("a", 1);
        cache->put("b", 2);
        cache->put("c", 3);

        cout << cache->get("a") << endl; // 1

        cache->put("d", 4);
        
        cout << cache->get("b") << endl; // 0 (null equivalent)
    }
};

int main() {
    LRUCacheDemo::main();
    return 0;
}