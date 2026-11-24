package me.whereareiam.toolkit.shipshape.rule.flow

import com.sun.source.tree.BinaryTree
import com.sun.source.tree.BreakTree
import com.sun.source.tree.CatchTree
import com.sun.source.tree.ClassTree
import com.sun.source.tree.ConditionalExpressionTree
import com.sun.source.tree.ContinueTree
import com.sun.source.tree.DoWhileLoopTree
import com.sun.source.tree.EnhancedForLoopTree
import com.sun.source.tree.ForLoopTree
import com.sun.source.tree.IfTree
import com.sun.source.tree.LambdaExpressionTree
import com.sun.source.tree.MethodTree
import com.sun.source.tree.ParenthesizedTree
import com.sun.source.tree.SwitchExpressionTree
import com.sun.source.tree.SwitchTree
import com.sun.source.tree.Tree
import com.sun.source.tree.WhileLoopTree
import com.sun.source.util.TreeScanner

/**
 * The cognitive complexity of a method, as SonarSource defines it: how hard its flow is to follow. A break in the
 * flow counts one (`if`, `else`, a ternary, `switch`, a loop, `catch`, a jump to a label, a run of `&&` or of `||`),
 * and a structure that stands inside another counts its depth on top. A lambda deepens what stands in it; a class
 * declared in the method is a method of its own and not counted here.
 */
internal class Complexity private constructor() : TreeScanner<Unit?, Unit?>() {
	private var total = 0
	private var depth = 0

	override fun visitIf(node: IfTree, unused: Unit?): Unit? = branch(node, nested = true)

	override fun visitConditionalExpression(node: ConditionalExpressionTree, unused: Unit?): Unit? {
		total += 1 + depth
		scan(node.condition, null)

		return deeper {
			scan(node.trueExpression, null)
			scan(node.falseExpression, null)
		}
	}

	override fun visitSwitch(node: SwitchTree, unused: Unit?): Unit? = structure { super.visitSwitch(node, null) }

	override fun visitSwitchExpression(node: SwitchExpressionTree, unused: Unit?): Unit? =
		structure { super.visitSwitchExpression(node, null) }

	override fun visitForLoop(node: ForLoopTree, unused: Unit?): Unit? = structure { super.visitForLoop(node, null) }

	override fun visitEnhancedForLoop(node: EnhancedForLoopTree, unused: Unit?): Unit? =
		structure { super.visitEnhancedForLoop(node, null) }

	override fun visitWhileLoop(node: WhileLoopTree, unused: Unit?): Unit? = structure { super.visitWhileLoop(node, null) }

	override fun visitDoWhileLoop(node: DoWhileLoopTree, unused: Unit?): Unit? = structure { super.visitDoWhileLoop(node, null) }

	override fun visitCatch(node: CatchTree, unused: Unit?): Unit? = structure { super.visitCatch(node, null) }

	override fun visitLambdaExpression(node: LambdaExpressionTree, unused: Unit?): Unit? =
		deeper { super.visitLambdaExpression(node, null) }

	override fun visitBreak(node: BreakTree, unused: Unit?): Unit? {
		if (node.label != null) total++

		return null
	}

	override fun visitContinue(node: ContinueTree, unused: Unit?): Unit? {
		if (node.label != null) total++

		return null
	}

	override fun visitBinary(node: BinaryTree, unused: Unit?): Unit? {
		if (node.kind !in LOGICAL) return super.visitBinary(node, null)

		total += runs(node, null)
		operands(node).forEach { scan(it, null) }

		return null
	}

	override fun visitClass(node: ClassTree, unused: Unit?): Unit? = null

	/** An `if` and what hangs from it: an `else if` counts one without its depth, an `else` one. */
	private fun branch(node: IfTree, nested: Boolean): Unit? {
		total += 1 + if (nested) depth else 0
		scan(node.condition, null)
		deeper { scan(node.thenStatement, null) }

		val otherwise = node.elseStatement ?: return null
		if (otherwise is IfTree) return branch(otherwise, nested = false)

		total++

		return deeper { scan(otherwise, null) }
	}

	private fun structure(inside: () -> Unit?): Unit? {
		total += 1 + depth

		return deeper(inside)
	}

	private fun deeper(inside: () -> Unit?): Unit? {
		depth++
		try {
			return inside()
		} finally {
			depth--
		}
	}

	/** How many runs of one operator the logical expression [node] is made of, [outer] being the operator around it. */
	private fun runs(node: BinaryTree, outer: Tree.Kind?): Int {
		val own = if (node.kind == outer) 0 else 1

		return own + listOf(node.leftOperand, node.rightOperand).sumOf { operand ->
			val logical = unwrapped(operand) as? BinaryTree

			if (logical != null && logical.kind in LOGICAL) runs(logical, node.kind) else 0
		}
	}

	/** What the logical expression [node] is made of beyond its own operators: the operands that are no `&&` or `||`. */
	private fun operands(node: BinaryTree): List<Tree> = listOf(node.leftOperand, node.rightOperand).flatMap { operand ->
		val logical = unwrapped(operand) as? BinaryTree

		if (logical != null && logical.kind in LOGICAL) operands(logical) else listOf(operand)
	}

	private fun unwrapped(tree: Tree): Tree = if (tree is ParenthesizedTree) unwrapped(tree.expression) else tree

	companion object {
		private val LOGICAL = setOf(Tree.Kind.CONDITIONAL_AND, Tree.Kind.CONDITIONAL_OR)

		/** The cognitive complexity of [method]: of its body, and none for a method without one. */
		fun of(method: MethodTree): Int {
			val counter = Complexity()
			counter.scan(method.body, null)

			return counter.total
		}
	}
}
