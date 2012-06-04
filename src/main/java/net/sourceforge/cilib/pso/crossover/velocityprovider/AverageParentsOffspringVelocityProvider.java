/**
 * Computational Intelligence Library (CIlib)
 * Copyright (C) 2003 - 2010
 * Computational Intelligence Research Group (CIRG@UP)
 * Department of Computer Science
 * University of Pretoria
 * South Africa
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, see <http://www.gnu.org/licenses/>.
 */
package net.sourceforge.cilib.pso.crossover.velocityprovider;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import java.util.List;
import net.sourceforge.cilib.entity.Particle;
import net.sourceforge.cilib.type.types.container.StructuredType;
import net.sourceforge.cilib.type.types.container.Vector;
import net.sourceforge.cilib.util.Vectors;

public class AverageParentsOffspringVelocityProvider implements OffspringVelocityProvider {
    @Override
    public StructuredType get(List<Particle> parent, Particle offspring) {
        return Vectors.mean(Lists.transform(parent, new Function<Particle, Vector>() {
            @Override
            public Vector apply(Particle f) {
                return (Vector) f.getVelocity();
            }            
        }));
    }
}
