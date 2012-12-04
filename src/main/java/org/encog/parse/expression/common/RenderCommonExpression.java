package org.encog.parse.expression.common;

import org.encog.ml.prg.EncogProgram;
import org.encog.ml.prg.epl.OpCodeHeader;
import org.encog.ml.prg.extension.KnownConst;
import org.encog.ml.prg.extension.ProgramExtensionTemplate;
import org.encog.ml.prg.extension.StandardExtensions;
import org.encog.parse.expression.ExpressionNodeType;
import org.encog.util.stack.StackString;

public class RenderCommonExpression {
	private EncogProgram program;
	private OpCodeHeader header = new OpCodeHeader();
	private StackString stack = new StackString(100);

	public RenderCommonExpression() {
	}

	public String render(final EncogProgram theProgram) {
		this.program = theProgram;
		this.program.setProgramCounter(0);
		return renderNode();
	}

	private void handleConst() {
		switch(this.header.getOpcode()) {
			case StandardExtensions.OPCODE_CONST_INT:
				stack.push(""+((int)this.header.getParam1()));
				break;
			case StandardExtensions.OPCODE_CONST_FLOAT:
				double d = this.program.readDouble();
				stack.push(""+this.program.getContext().getFormat().format(d,32));
				break;
			default:
				stack.push("[Unknown Constant]");
				break;
		}
		
	}
	
	private void handleConstKnown() {
		ProgramExtensionTemplate temp = this.program.getContext().getFunctions().getOpCode(this.header.getOpcode());
		stack.push(temp.getName());
	}

	private void handleVar() {
		int varIndex = (int)this.header.getParam2();
		stack.push(this.program.getVariables().getVariableName(varIndex));
	}
	
	private void handleFunction() {
		int opcode = this.header.getOpcode();
		ProgramExtensionTemplate temp = this.program.getContext().getFunctions().getOpCode(opcode);
		
		StringBuilder result = new StringBuilder();
		result.append(temp.getName());
		result.append('(');
		for(int i=0;i<temp.getChildNodeCount();i++) {
			if( i>0 ) {
				result.append(',');
			}
			result.append(this.stack.pop());
		}
		result.append(')');		
		this.stack.push(result.toString());
	}
	
	private void handleOperator() {
		int opcode = this.header.getOpcode();
		ProgramExtensionTemplate temp = this.program.getContext().getFunctions().getOpCode(opcode);
		
		StringBuilder result = new StringBuilder();
		String a = this.stack.pop();
		String b = this.stack.pop();
		result.append("(");
		result.append(b);
		result.append(temp.getName());
		result.append(a);
		result.append(")");
		
		this.stack.push(result.toString());
	}

	public ExpressionNodeType determineNodeType() {
		this.program.readNodeHeader(this.header);
		int opcode = this.header.getOpcode();
		ProgramExtensionTemplate temp = this.program.getContext().getFunctions().getOpCode(opcode);
		
		if( temp instanceof KnownConst ) {
			return ExpressionNodeType.ConstKnown;
		}
		
		if (opcode==StandardExtensions.OPCODE_CONST_FLOAT || opcode==StandardExtensions.OPCODE_CONST_INT) {
			return ExpressionNodeType.ConstVal;
		}  
			
		if (opcode==StandardExtensions.OPCODE_VAR ) {
			return ExpressionNodeType.Variable;
		} 
		
		if( temp.getChildNodeCount()!=2 ) {
			return ExpressionNodeType.Function;
		}
		
		String name = temp.getName();
		
		if( !Character.isLetterOrDigit(name.charAt(0)) ) {
			return ExpressionNodeType.Operator;			
		}
		
		return ExpressionNodeType.Function;		
	}

	private String renderNode() {
		this.program.setProgramCounter(0);
		
		while(!this.program.eof()) {
			switch (determineNodeType()) {
			case ConstVal:
				handleConst();
				break;
			case ConstKnown:
				handleConstKnown();
				break;
			case Operator:
				handleOperator();
				break;
			case Variable:
				handleVar();
				break;
			case Function:
				handleFunction();
				break;
			}
		}
		return this.stack.pop();
	}
}