# 编译器文档

## 参考编译器介绍

编译器总体结构设计参考了[GitHub - No-SF-Work/ayame: A compiler that translates SysY (a subset of C language) into ARMv7a, implemented in Java15.](https://github.com/No-SF-Work/ayame)（2021年北航参加编译比赛的一等奖作品），以及参考上述编译器设计的2022年我参加编译竞赛合作完成的编译器。同时参考了[《crafting interpreter》](http://www.craftinginterpreters.com/)的词法分析器实现，总结了自己之前实现过的词法分析器的实现，[sloth-lang: 出于学习目的编写的基于字节码的解释器。](https://gitee.com/aaicy64/sloth-lang)

以Ayame为例（2021年北航参加编译比赛的一等奖作品），分析其结构与组织

### UML

![ayame_UML](D:\course2022aut\compile\expriment\ayame_UML.png)

### 文件结构

![image-20221206195526680](C:\Users\11067\AppData\Roaming\Typora\typora-user-images\image-20221206195526680.png)

可以看到主要分为前端（frontend），中端（ir），后端（backend），前端使用ANTLR自动生成，中端采用了类似LLVM IR的SSA形式中间代码，优化被组织为Pass（遍），可以方便调度优化的顺序和开关优化。

IR在内存中的表示为User，Value互相引用的结构（参考LLVM IR），我在课设中也采用了这种设计，将在之后详细说明。

Pass和User，Value的设计是参考的重点。

### 前端参考

因为比赛不要求错误处理，且词法分析和语法分析部分可以用ANTLR等生成器生成，所以前端无法参考Ayame。于是参考了一本较好的编译器设计书籍[《crafting interpreter》](http://www.craftinginterpreters.com/)。它实现了一种类Lua的解释型语言，前端使用手写的自顶向下分析器，并使用了算符优先分析解析表达式，后端实现了栈式虚拟机（pcode可参考），并且实现了词法闭包，标记-清理垃圾回收和面向对象等高级功能。

## 编译器总体设计介绍

### 语言选择

选择Java，虽然Java8缺少很多特性，有些代码显得比较冗长，相对而言C++11更加现代，性能也更好，但是之后代码生成和优化部分可能需要维护比较复杂的数据结构（图），Java自动内存管理可以降低编码难度。

### UML图

![IR_UML](D:\course2022aut\compile\expriment\Ayaya\IR_UML.png)

IR设计最终参考的是LLVM IR的`Value`，`User`的继承层次结构。（见上图Value为根的继承树）

后端中间代码采用一种很接近MIPS汇编的形式，除了使用虚拟寄存器和扩展了Phi指令和并行复制指令（用于支持SSA）外，其他和MIPS完全相同。（见上图MCInstr为根的继承树）

Pass为编译器运行的“遍”，用于计算信息（支配，活跃等）和优化（GVNGCM，死代码删除等）。相应的MCPass也是“遍”，区别于Pass只是运行在后端中间代码上，而不是中端中间代码上。

Ty为类型，相对于LLVM IR复杂的类型系统，由于我们只支持整型和整形数组，做了相应的简化。

Reg为寄存器，PReg为物理寄存器，VReg为虚拟寄存器，ValueReg只是在后端代码生成过程中用于保存变量映射关系的中间数据结构。它们构成了如图的继承关系。

还有其他负责词法，语法分析，错误处理等功能的类。

中端代码的设计和Pass的组织参考了Ayame，前端为自己设计的适应课设迭代开发要求的组织结构，后端也在参考Ayame的ARM后端的基础上自行实现。



## 词法分析

主要参考了[《crafting interpreter》](http://www.craftinginterpreters.com/)的词法分析器实现，总结了自己之前实现过的词法分析器的实现，[sloth-lang: 出于学习目的编写的基于字节码的解释器。](https://gitee.com/aaicy64/sloth-lang)

整个词法分析器是一个while{switch{...}}结构，对于关键字和标识符，采用直接识别而不是真的构建DFA，这样比较便于维护，对于不可能冲突的单字符，如+，-，直接识别，对于可能冲突的单双字符进行特判，如/，/*，//。此外需要处理数字串、注释和格式字符串等。

## 语法分析

递归下降法配合适当的Look forward，因为输出语法树形式要求的限定，没有使用可以简化表达式识别的Pratt Parser（自顶向下算符优先级，其实就是OPG，这大概会把表达式树展平，不太好处理）。

最终的设计中，对赋值语句采用了回溯的方式（和表达式语句冲突），对表达式采用了改写文法（EBNF）然后对语法树进行变形的方式。

## 错误处理相关

对于可能（因为错误）不存在的Token，如`)`，`]`，`;`，不能作为判断语法成分的条件。因为错误处理需要建立符号表，所以是和中间代码生成一起写的（词法分析和语法分析也处理一些错误）。



## 中间代码生成

采用简化的LLVM IR，先生成load，store形式，再通过mem2reg化为SSA形式。

## 后端代码生成

φ函数的解构使用Brigg的算法（基于φ等价类的需要构建冲突图，且不好理解，标准方式会划分关键边，产生很多跳转）。
实际完成后补充：还是采用了拆关键边的算法，因为不确定φ的liveIn，liveOut语义，论文使用了liveOut
但是没有明确φ的活跃计算的方式，稳妥起见还是不用了。

寄存器分配采用图染色法。在第一次代码生成还没有实现，暂时没有分配寄存器，临时变量全部放在栈上。

尽量使用伪指令，要不白白浪费一个寄存器（$at）。
