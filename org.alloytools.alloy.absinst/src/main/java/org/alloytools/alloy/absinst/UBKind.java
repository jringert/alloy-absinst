package org.alloytools.alloy.absinst;


public enum UBKind {
                    /**
                     * no upper bound given
                     * <ul>
                     * <li>an abstract instance might not exist</li>
                     * </ul>
                     */
                    NO_UPPER,

                    /**
                     * upper bound is exactly the atoms and tuples of the original instance
                     * <ul>
                     * <li>this means we always have a UB</li>
                     * </ul>
                     */
                    INSTANCE,

                    /**
                     * upper bound is unrestricted or the tuples of the original instance for every
                     * signature and every relation individually, i.e., on signature might be bound
                     * by the instance while another might have no upper bound
                     * <ul>
                     * <li>this is better for performance than <code>EXACT</code> UB</li>
                     * </ul>
                     */
                    INSTANCE_OR_NO_UPPER,

                    /**
                     * exact upper bound computed for every signature and field
                     */
                    EXACT

}
