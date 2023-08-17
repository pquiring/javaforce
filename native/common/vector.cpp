template<class T>
class Vector {
  private:
    int alloc;
    int size;
    T *array;
  public:
    Vector() {
      alloc = 0;
      size = 0;
      array = (T*)NULL;
    }
    ~Vector() {
      if (alloc > 0) {
        free(array);
      }
    }
    int getSize() {return size;}
    void add(T t) {
      if (size == alloc) {
        if (alloc == 0) {
          alloc = 16;
          array = (T*) malloc(sizeof(T) * alloc);
        } else {
          alloc <<= 1;
          array = (T*) realloc(t, sizeof(T) * alloc);
        }
      }
      array[size++] = t;
    }
    T* get() {
      return array;
    }
    T getAt(int idx) {
      return array[idx];
    }
};
