# 优化文档

## 优化目标

优化目标是MARS，所以优化应该针对其单周期的特点以及计分标准（加权指令计数），所以指令调度类优化没有效果，不执行的代码不删除也不会有影响。优化任务时间有限，一些优化虽然对于产品级编译器是必要的，但是课设可以不做。

## IR设计

IR的设计对于优化的实现有重要的作用，这里我使用了模仿LLVM IR的一种更为简化的SSA（静态单赋值）形式的IR。形式上如下：

```
%long_array(%0:int,):int{
%1:
	%2 = alloc Stack int[10000]
	jmp %10
%4:
	%5 = mul %11,%11
	%6 = mod %5,10
	store %6,%2[%11]
	%8 = add %11,1
	jmp %10
%10:
	%11 = phi [0,%1][%8,%4]
	%12 = slt %11,10000
	br %12,%4,%14
%14:
	%15 = alloc Stack int[10000]
	jmp %24
%17:
	%18= load %2[%25]
	%19 = mul %18,%18
	%20 = mod %19,10
	store %20,%15[%25]
	%22 = add %25,1
	jmp %24
	...
```

IR特点：

- 保留了函数的结构
- 包含基本块
- 指令形式上是四元式

IR主要数据结构：

```mermaid
classDiagram
direction BT
class BasicBlock {
  + List~BasicBlock~ domer
  - MyList~Instr~ list
  + List~BasicBlock~ df
  + List~BasicBlock~ prec
  + int domDepth
  + BasicBlock idomer
  + List~BasicBlock~ succ
  + List~BasicBlock~ idoming
  + int loopDepth
  + List~BasicBlock~ doming
}
class CompUnit {
  - MyList~AllocInstr~ globalValueList
  - MyList~Function~ list
}
class Function {
  + AllocInstr retAlloc
  - List~Param~ params
  - boolean isRecursive
  - boolean isPure
  - MyList~BasicBlock~ list
}
class Instr {
  + boolean isNop
}

BasicBlock "1" *--> "list *" Instr 
CompUnit "1" *--> "list *" Function 
Function "1" *--> "list *" BasicBlock 

```

形成指令-基本块-函数-编译单元的层次。

### Value，User与Def-Use链（网？）

模仿LLVM IR实现的Value和User的继承结构是IR实现的核心，是各种优化的基础。

```mermaid
classDiagram
direction BT
class AllocInstr
class ArrView
class BasicBlock
class BinaryOp
class BrInstr
class BuiltinCallInstr
class CallInstr
class CompUnit
class Constant
class Function
class IRCloner
class InitVal
class Instr
class JmpInstr
class LoadInstr
class MyString
class Param
class PhiInstr
class RetInstr
class StoreInstr
class Undef
class User
class Value

AllocInstr  -->  Instr 
ArrView  -->  Instr 
BasicBlock  -->  Value 
BinaryOp  -->  Instr 
BrInstr  -->  Instr 
BuiltinCallInstr  -->  Instr 
CallInstr  -->  Instr 
Function  -->  User 
InitVal  -->  Value 
Instr  -->  User 
JmpInstr  -->  Instr 
LoadInstr  -->  Instr 
MyString  -->  Value 
Param  -->  Value 
PhiInstr  -->  Instr 
RetInstr  -->  Instr 
StoreInstr  -->  Instr 
Undef  -->  Value 
User  -->  Value 

```

User顾名思义就是会使用其他值的值（它也继承值，User也可能被使用）。

例如，Instruction是User，对于一个二元运算指令BinaryOp，它use它的两个操作数，并被使用它的结果的指令使用，IR需要维护一个值的使用者和使用信息，比如

```
%1 = add 1, 1
%2 = add 2, 2
%3 = add %1, %2
%4 = add %3, %2
```

对于`%3 = add %1, %2` 它维护了对第一行和第二行的指令的引用，并且因为它被第四行代码使用，它也维护了对第四行指令的引用，即

```
Object %3{
	uses: [%1, %2]
	users: [%4]
}
```

这种形式可以方便优化，这里还有几个在Value和User上的方法：

```
void replaceUseWith(Value old, Value nnew)
void removeMeFromAllMyUses() 
void replaceAllUsesOfMeWith(Value other)
```

分别为：1.将对旧值的使用替换为新值，2.从使用的值的users列表中移出自己，通常用于这个值被删除了（比如死代码）3.将对自己的使用替换为对另一个值的使用，通常用于值标号合并公共子表达式，比如另一个值（指令）也算出来一样的值，我们就替换过来然后删掉这条冗余指令。

通过维护上述uses，user关系，上述操作（也维护了这些关系）能够以较高的效率实现。

## 中端优化

中端优化是在上述IR上进行的。

### 指令简化

对于所以分支值都一样的phi指令（phi指令：SSA特有，例如`%5 = phi [%1, %2] [%3, %4]`，则phi指令值取%1如果从%2基本块跳转来，取%3如果从基本块%4跳转来，用于保证SSA性质），可以合并。

对于形如`a+1+2`， `a-1-2`这样的式子可以合并常数，一般的常量传播由于产生的表达式树形状可能比较容易合并`1+2+a`这样的式子，上述形式需要单独处理。树大概会长这样：

```
(+ (+ a 1) 2)
```

如果只考虑一个表达式子树（如（+ a 1）），则难以化简。除了上面的加减法，可能也有其他更复杂的算术指令简化方式，比如乘除，甚至可能可以运用分配律。

连续的加同一个常数如果超过乘法的代价，可以简化成乘法。

### 控制流图简化

在代码生成过程中，可能生成一些空块，或者在优化过程中产生一些无法到达的块，或者是唯一前驱后继的两块等一些冗余的块或跳转，可以删除或者合并。

### 函数内联

可以将短函数内联（长函数也可以其实，但是可能由于冲突过多影响图着色），函数内联的意义不仅在于减少调用开销，还可以暴露更多优化机会，比如把一些运算通过函数内联放在了一起，可以暴露更多运算优化。

### 全局常量识别

有一些从未被写过的全局变量并没有被标成`const`，但是可以在中端识别（User中只有load，没有store），和函数内联一样，也可以暴露更多优化。

### 简单死代码删除

一个没有使用者的指令是无用的，可以删除，这个过程是迭代的，删除一条指令也会将它使用的值加入工作表，看它使用的值是不是也可以被删除，直到工作表为空。

算法来自《现代编译原理》SSA主题。

### 简单常数传播

### GVNGCM（全局值标号和全局代码移动）

### Mem2Reg

## 后端优化

