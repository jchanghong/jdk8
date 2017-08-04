

package java.security;

import java.util.Set;



public interface AlgorithmConstraints {


    public boolean permits(Set<CryptoPrimitive> primitives,
            String algorithm, AlgorithmParameters parameters);


    public boolean permits(Set<CryptoPrimitive> primitives, Key key);


    public boolean permits(Set<CryptoPrimitive> primitives,
                String algorithm, Key key, AlgorithmParameters parameters);

}
