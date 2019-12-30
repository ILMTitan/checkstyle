////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2019 the original author or authors.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////

package com.puppycrawl.tools.checkstyle.checks.blocks;

import java.util.Locale;

import com.puppycrawl.tools.checkstyle.DetailAstImpl;
import com.puppycrawl.tools.checkstyle.StatelessCheck;
import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.utils.CheckUtil;
import com.puppycrawl.tools.checkstyle.utils.CommonUtil;

/**
 * <p>
 * Checks the placement of right curly braces (<code>'}'</code>)
 * for if-else, try-catch-finally blocks, while-loops, for-loops,
 * method definitions, class definitions, constructor definitions,
 * instance, static initialization blocks and annotation definitions.
 * For right curly brace of expression blocks please follow issue
 * <a href="https://github.com/checkstyle/checkstyle/issues/5945">#5945</a>.
 * </p>
 * <ul>
 * <li>
 * Property {@code option} - Specify the policy on placement of a right curly brace
 * (<code>'}'</code>).
 * Default value is {@code same}.
 * </li>
 * <li>
 * Property {@code tokens} - tokens to check
 * Default value is:
 * <a href="https://checkstyle.org/apidocs/com/puppycrawl/tools/checkstyle/api/TokenTypes.html#LITERAL_TRY">
 * LITERAL_TRY</a>,
 * <a href="https://checkstyle.org/apidocs/com/puppycrawl/tools/checkstyle/api/TokenTypes.html#LITERAL_CATCH">
 * LITERAL_CATCH</a>,
 * <a href="https://checkstyle.org/apidocs/com/puppycrawl/tools/checkstyle/api/TokenTypes.html#LITERAL_FINALLY">
 * LITERAL_FINALLY</a>,
 * <a href="https://checkstyle.org/apidocs/com/puppycrawl/tools/checkstyle/api/TokenTypes.html#LITERAL_IF">
 * LITERAL_IF</a>,
 * <a href="https://checkstyle.org/apidocs/com/puppycrawl/tools/checkstyle/api/TokenTypes.html#LITERAL_ELSE">
 * LITERAL_ELSE</a>.
 * </li>
 * </ul>
 * <p>
 * To configure the check:
 * </p>
 * <pre>
 * &lt;module name="RightCurly"/&gt;
 * </pre>
 * <p>
 * To configure the check with policy {@code alone} for {@code else} and
 * <a href="https://checkstyle.org/apidocs/com/puppycrawl/tools/checkstyle/api/TokenTypes.html#METHOD_DEF">
 * METHOD_DEF</a> tokens:
 * </p>
 * <pre>
 * &lt;module name=&quot;RightCurly&quot;&gt;
 *   &lt;property name=&quot;option&quot; value=&quot;alone&quot;/&gt;
 *   &lt;property name=&quot;tokens&quot; value=&quot;LITERAL_ELSE, METHOD_DEF&quot;/&gt;
 * &lt;/module&gt;
 * </pre>
 *
 * @since 3.0
 *
 */
@StatelessCheck
public class RightCurlyCheck extends AbstractCheck {

    /**
     * A key is pointing to the warning message text in "messages.properties"
     * file.
     */
    public static final String MSG_KEY_LINE_BREAK_BEFORE = "line.break.before";

    /**
     * A key is pointing to the warning message text in "messages.properties"
     * file.
     */
    public static final String MSG_KEY_LINE_ALONE = "line.alone";

    /**
     * A key is pointing to the warning message text in "messages.properties"
     * file.
     */
    public static final String MSG_KEY_LINE_SAME = "line.same";

    /**
     * Specify the policy on placement of a right curly brace (<code>'}'</code>).
     */
    private RightCurlyOption option = RightCurlyOption.SAME;

    /**
     * Setter to specify the policy on placement of a right curly brace (<code>'}'</code>).
     * @param optionStr string to decode option from
     * @throws IllegalArgumentException if unable to decode
     */
    public void setOption(String optionStr) {
        option = RightCurlyOption.valueOf(optionStr.trim().toUpperCase(Locale.ENGLISH));
    }

    @Override
    public int[] getDefaultTokens() {
        return new int[] {
            TokenTypes.LITERAL_TRY,
            TokenTypes.LITERAL_CATCH,
            TokenTypes.LITERAL_FINALLY,
            TokenTypes.LITERAL_IF,
            TokenTypes.LITERAL_ELSE,
        };
    }

    @Override
    public int[] getAcceptableTokens() {
        return new int[] {
            TokenTypes.LITERAL_TRY,
            TokenTypes.LITERAL_CATCH,
            TokenTypes.LITERAL_FINALLY,
            TokenTypes.LITERAL_IF,
            TokenTypes.LITERAL_ELSE,
            TokenTypes.CLASS_DEF,
            TokenTypes.METHOD_DEF,
            TokenTypes.CTOR_DEF,
            TokenTypes.LITERAL_FOR,
            TokenTypes.LITERAL_WHILE,
            TokenTypes.LITERAL_DO,
            TokenTypes.STATIC_INIT,
            TokenTypes.INSTANCE_INIT,
            TokenTypes.ANNOTATION_DEF,
        };
    }

    @Override
    public int[] getRequiredTokens() {
        return CommonUtil.EMPTY_INT_ARRAY;
    }

    @Override
    public void visitToken(DetailAST ast) {
        final Details details = Details.getDetails(ast);
        final DetailAST rcurly = details.rcurly;

        if (rcurly != null) {
            final String violation = validate(details);
            if (!violation.isEmpty()) {
                log(rcurly, violation, "}", rcurly.getColumnNo() + 1);
            }
        }
    }

    /**
     * Does general validation.
     * @param details for validation.
     * @return violation message or empty string
     *     if there was not violation during validation.
     */
    private String validate(Details details) {
        String violation = "";
        if (shouldHaveLineBreakBefore(option, details)) {
            violation = MSG_KEY_LINE_BREAK_BEFORE;
        }
        else if (shouldBeOnSameLine(option, details)) {
            violation = MSG_KEY_LINE_SAME;
        }
        else if (shouldBeAloneOnLine(details, getLine(details.rcurly.getLineNo() - 1))) {
            violation = MSG_KEY_LINE_ALONE;
        }
        return violation;
    }

    /**
     * Checks whether a right curly should have a line break before.
     * @param bracePolicy option for placing the right curly brace.
     * @param details details for validation.
     * @return true if a right curly should have a line break before.
     */
    private static boolean shouldHaveLineBreakBefore(RightCurlyOption bracePolicy,
                                                     Details details) {
        return bracePolicy == RightCurlyOption.SAME
                && !hasLineBreakBefore(details.rcurly)
                && details.lcurly.getLineNo() != details.rcurly.getLineNo();
    }

    /**
     * Checks that a right curly should be on the same line as the next statement.
     * @param bracePolicy option for placing the right curly brace
     * @param details Details for validation
     * @return true if a right curly should be alone on a line.
     */
    private static boolean shouldBeOnSameLine(RightCurlyOption bracePolicy, Details details) {
        return bracePolicy == RightCurlyOption.SAME
                && !details.rcurlyEndsSyntax
                && details.rcurly.getLineNo() != details.nextToken.getLineNo();
    }

    /**
     * Checks that a right curly should be alone on a line.
     * @param details Details for validation
     * @param targetSrcLine A string with contents of rcurly's line
     * @return true if a right curly should be alone on a line.
     */
    private boolean shouldBeAloneOnLine(Details details, String targetSrcLine) {
        final boolean isViolation;
        switch (option) {
            case ALONE:
                isViolation = !isAloneOnLine(details, targetSrcLine);
                break;
            case ALONE_OR_EMPTY:
                isViolation = !isAloneOnLine(details, targetSrcLine)
                    && !isEmptyBlock(details);
                break;
            case ALONE_OR_SINGLELINE:
                isViolation = !isAloneOnLine(details, targetSrcLine)
                    && !isBlockAloneOnSingleLine(details);
                break;
            default:
                // option == SAME
                isViolation = details.rcurlyEndsSyntax
                    && !isAloneOnLine(details, targetSrcLine)
                    && !isBlockAloneOnSingleLine(details);
                break;
        }
        return isViolation;
    }

    private static boolean isEmptyBlock(Details details) {

        final DetailAST emptyRcurlyLocation;
        if (details.lcurly.getParent().getType() == TokenTypes.OBJBLOCK) {
            emptyRcurlyLocation = details.lcurly.getNextSibling();
        }
        else {
            emptyRcurlyLocation = details.lcurly.getFirstChild();
        }
        return emptyRcurlyLocation == details.rcurly;
    }

    /**
     * Checks whether right curly is alone on a line.
     * @param details for validation.
     * @param targetSrcLine A string with contents of rcurly's line
     * @return true if right curly is alone on a line.
     */
    private static boolean isAloneOnLine(Details details, String targetSrcLine) {
        final DetailAST rcurly = details.rcurly;
        final DetailAST nextToken = details.nextToken;
        return (rcurly.getLineNo() != nextToken.getLineNo() || skipDoubleBraceInstInit(details))
                && CommonUtil.hasWhitespaceBefore(details.rcurly.getColumnNo(), targetSrcLine);
    }

    /**
     * This method determines if the double brace initialization should be skipped over by the
     * check. Double brace initializations are treated differently. The corresponding inner
     * rcurly is treated as if it was alone on line even when it may be followed by another
     * rcurly and a semi, raising no violations.
     * <i>Please do note though that the line should not contain anything other than the following
     * right curly and the semi following it or else violations will be raised.</i>
     * Only the kind of double brace initializations shown in the following example code will be
     * skipped over:<br>
     * <pre>
     *     {@code Map<String, String> map = new LinkedHashMap<>() {{
     *           put("alpha", "man");
     *       }}; // no violation}
     * </pre>
     *
     * @param details {@link Details} object containing the details relevant to the rcurly
     * @return if the double brace initialization rcurly should be skipped over by the check
     */
    private static boolean skipDoubleBraceInstInit(Details details) {
        final DetailAST rcurly = details.rcurly;
        final DetailAST tokenAfterNextToken = Details.getNextToken(details.nextToken);
        return rcurly.getParent().getParent().getType() == TokenTypes.INSTANCE_INIT
                && details.nextToken.getType() == TokenTypes.RCURLY
                && rcurly.getLineNo() != Details.getNextToken(tokenAfterNextToken).getLineNo();
    }

    /**
     * Checks whether block has a single-line format and is alone on a line.
     * @param details for validation.
     * @return true if block has single-line format and is alone on a line.
     */
    private static boolean isBlockAloneOnSingleLine(Details details) {
        final DetailAST rcurly = details.rcurly;
        final DetailAST lcurly = details.lcurly;
        DetailAST nextToken = details.nextToken;
        while (nextToken.getType() == TokenTypes.LITERAL_ELSE) {
            nextToken = Details.getNextToken(nextToken);
        }
        if (nextToken.getType() == TokenTypes.DO_WHILE) {
            final DetailAST doWhileSemi = nextToken.getParent().getLastChild();
            nextToken = Details.getNextToken(doWhileSemi);
        }
        return rcurly.getLineNo() == lcurly.getLineNo()
                && (rcurly.getLineNo() != nextToken.getLineNo()
                || isRightcurlyFollowedBySemicolon(details));
    }

    /**
     * Checks whether the right curly is followed by a semicolon.
     * @param details details for validation.
     * @return true if the right curly is followed by a semicolon.
     */
    private static boolean isRightcurlyFollowedBySemicolon(Details details) {
        return details.nextToken.getType() == TokenTypes.SEMI;
    }

    /**
     * Checks if right curly has line break before.
     * @param rightCurly right curly token.
     * @return true, if right curly has line break before.
     */
    private static boolean hasLineBreakBefore(DetailAST rightCurly) {
        DetailAST previousToken = rightCurly.getPreviousSibling();
        if (previousToken == null) {
            previousToken = rightCurly.getParent();
        }
        return rightCurly.getLineNo() != previousToken.getLineNo();
    }

    /**
     * Structure that contains all details for validation.
     */
    private static final class Details {

        /** Right curly. */
        private final DetailAST rcurly;
        /** Left curly. */
        private final DetailAST lcurly;
        /** Next token. */
        private final DetailAST nextToken;
        /** True if the right curly terminates the syntax. */
        private final boolean rcurlyEndsSyntax;

        /**
         * Constructor.
         * @param lcurly the lcurly of the token whose details are being collected
         * @param rcurly the rcurly of the token whose details are being collected
         * @param nextToken the token after the token whose details are being collected
         * @param rcurlyEndsSyntax boolean value to determine if to check last rcurly
         */
        private Details(DetailAST lcurly, DetailAST rcurly,
                        DetailAST nextToken, boolean rcurlyEndsSyntax) {
            this.lcurly = lcurly;
            this.rcurly = rcurly;
            this.nextToken = nextToken;
            this.rcurlyEndsSyntax = rcurlyEndsSyntax;
        }

        /**
         * Collects validation Details.
         * @param ast a {@code DetailAST} value
         * @return object containing all details to make a validation
         */
        private static Details getDetails(DetailAST ast) {
            final Details details;
            switch (ast.getType()) {
                case TokenTypes.LITERAL_TRY:
                case TokenTypes.LITERAL_CATCH:
                case TokenTypes.LITERAL_FINALLY:
                    details = getDetailsForTryCatchFinally(ast);
                    break;
                case TokenTypes.LITERAL_IF:
                case TokenTypes.LITERAL_ELSE:
                    details = getDetailsForIfElse(ast);
                    break;
                case TokenTypes.LITERAL_DO:
                case TokenTypes.LITERAL_WHILE:
                case TokenTypes.LITERAL_FOR:
                    details = getDetailsForLoops(ast);
                    break;
                default:
                    details = getDetailsForOthers(ast);
                    break;
            }
            return details;
        }

        /**
         * Collects validation details for LITERAL_TRY, LITERAL_CATCH, and LITERAL_FINALLY.
         * @param ast a {@code DetailAST} value
         * @return object containing all details to make a validation
         */
        private static Details getDetailsForTryCatchFinally(DetailAST ast) {
            final DetailAST lcurly;
            DetailAST nextToken;
            final int tokenType = ast.getType();
            if (tokenType == TokenTypes.LITERAL_TRY) {
                if (ast.getFirstChild().getType() == TokenTypes.RESOURCE_SPECIFICATION) {
                    lcurly = ast.getFirstChild().getNextSibling();
                }
                else {
                    lcurly = ast.getFirstChild();
                }
                nextToken = lcurly.getNextSibling();
            }
            else {
                nextToken = ast.getNextSibling();
                lcurly = ast.getLastChild();
            }

            final boolean rcurlyEndsSyntax;
            if (nextToken == null) {
                rcurlyEndsSyntax = true;
                nextToken = getNextToken(ast);
            }
            else {
                rcurlyEndsSyntax = false;
            }

            final DetailAST rcurly = lcurly.getLastChild();
            return new Details(lcurly, rcurly, nextToken, rcurlyEndsSyntax);
        }

        /**
         * Collects validation details for LITERAL_IF and LITERAL_ELSE.
         * @param ast a {@code DetailAST} value
         * @return object containing all details to make a validation
         */
        private static Details getDetailsForIfElse(DetailAST ast) {
            final boolean rcurlyEndsSyntax;
            final DetailAST lcurly;
            DetailAST nextToken = ast.findFirstToken(TokenTypes.LITERAL_ELSE);

            if (nextToken == null) {
                rcurlyEndsSyntax = true;
                nextToken = getNextToken(ast);
                lcurly = ast.getLastChild();
            }
            else {
                rcurlyEndsSyntax = false;
                lcurly = nextToken.getPreviousSibling();
            }

            DetailAST rcurly = null;
            if (lcurly.getType() == TokenTypes.SLIST) {
                rcurly = lcurly.getLastChild();
            }
            return new Details(lcurly, rcurly, nextToken, rcurlyEndsSyntax);
        }

        /**
         * Collects validation details for CLASS_DEF, METHOD DEF, CTOR_DEF, STATIC_INIT,
         * INSTANCE_INIT and ANNOTATION_DEF.
         * @param ast a {@code DetailAST} value
         * @return an object containing all details to make a validation
         */
        private static Details getDetailsForOthers(DetailAST ast) {
            DetailAST rcurly = null;
            final DetailAST lcurly;
            final int tokenType = ast.getType();
            if (tokenType == TokenTypes.CLASS_DEF || tokenType == TokenTypes.ANNOTATION_DEF) {
                final DetailAST child = ast.getLastChild();
                lcurly = child.getFirstChild();
                rcurly = child.getLastChild();
            }
            else {
                lcurly = ast.findFirstToken(TokenTypes.SLIST);
                if (lcurly != null) {
                    // SLIST could be absent if method is abstract
                    rcurly = lcurly.getLastChild();
                }
            }
            return new Details(lcurly, rcurly, getNextToken(ast), true);
        }

        /**
         * Collects validation details for loops' tokens.
         * @param ast a {@code DetailAST} value
         * @return an object containing all details to make a validation
         */
        private static Details getDetailsForLoops(DetailAST ast) {
            DetailAST rcurly = null;
            final DetailAST lcurly;
            final DetailAST nextToken;
            final int tokenType = ast.getType();
            final boolean rcurlyEndsSyntax;
            if (tokenType == TokenTypes.LITERAL_DO) {
                rcurlyEndsSyntax = false;
                nextToken = ast.findFirstToken(TokenTypes.DO_WHILE);
                lcurly = ast.findFirstToken(TokenTypes.SLIST);
                if (lcurly != null) {
                    rcurly = lcurly.getLastChild();
                }
            }
            else {
                rcurlyEndsSyntax = true;
                lcurly = ast.findFirstToken(TokenTypes.SLIST);
                if (lcurly != null) {
                    // SLIST could be absent in code like "while(true);"
                    rcurly = lcurly.getLastChild();
                }
                nextToken = getNextToken(ast);
            }
            return new Details(lcurly, rcurly, nextToken, rcurlyEndsSyntax);
        }

        /**
         * Finds next token after the given one.
         * @param ast the given node.
         * @return the token which represents next lexical item.
         */
        private static DetailAST getNextToken(DetailAST ast) {
            DetailAST next = null;
            DetailAST parent = ast;
            while (next == null && parent != null) {
                next = parent.getNextSibling();
                parent = parent.getParent();
            }
            if (next == null) {
                // a DetailAST object with DetailAST#NOT_INITIALIZED for line and column numbers
                // that no 'actual' DetailAST objects can have.
                next = new DetailAstImpl();
            }
            else {
                next = CheckUtil.getFirstNode(next);
            }
            return next;
        }

    }

}
