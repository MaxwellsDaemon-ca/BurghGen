package com.chancema.burghgen.util;

import java.util.Random;

/**
 * A simple 2D noise generator based on a permutation-based gradient approach.
 * 
 * While not true Simplex Noise (as patented by Ken Perlin), this implementation
 * provides smooth, repeatable pseudorandom noise suitable for terrain generation,
 * texture variation, or other procedural content tasks.
 */
public class SimplexNoise {

    private final int[] perm;

    /**
     * Constructs a new SimplexNoise generator with a given seed.
     * 
     * Initializes a shuffled permutation table (512 entries) for gradient hashing.
     * 
     * @param seed the seed used for permutation shuffling
     */
    public SimplexNoise(long seed) {
        perm = new int[512];
        Random random = new Random(seed);
        int[] p = new int[256];

        // Initialize base permutation
        for (int i = 0; i < 256; i++) {
            p[i] = i;
        }

        // Shuffle the permutation array using Fisher–Yates
        for (int i = 255; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = p[i];
            p[i] = p[j];
            p[j] = tmp;
        }

        // Duplicate into 512-length array for overflow-safe indexing
        for (int i = 0; i < 512; i++) {
            perm[i] = p[i & 255];
        }
    }

    /**
     * Generates a noise value at the given 2D coordinates.
     * 
     * Produces smooth interpolation based on gradient hashing.
     * 
     * @param x the X coordinate
     * @param y the Y coordinate
     * @return a noise value between roughly -1.0 and 1.0
     */
    public double noise(double x, double y) {
        int X = (int) Math.floor(x) & 255;
        int Y = (int) Math.floor(y) & 255;

        x -= Math.floor(x);
        y -= Math.floor(y);

        double u = fade(x);
        double v = fade(y);

        int A = perm[X] + Y;
        int B = perm[X + 1] + Y;

        return lerp(v,
            lerp(u, grad(perm[A], x, y), grad(perm[B], x - 1, y)),
            lerp(u, grad(perm[A + 1], x, y - 1), grad(perm[B + 1], x - 1, y - 1))
        );
    }

    /**
     * Fade function for smoothing input values.
     * 
     * Applies the 6t^5 - 15t^4 + 10t^3 curve used in classic Perlin noise.
     * 
     * @param t input value
     * @return smoothed output
     */
    private double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    /**
     * Linear interpolation between two values.
     * 
     * @param t interpolation factor (0.0 to 1.0)
     * @param a start value
     * @param b end value
     * @return interpolated value
     */
    private double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    /**
     * Gradient function used to simulate directionality in noise.
     * 
     * Selects pseudo-gradients based on hash value.
     * 
     * @param hash hash code from permutation table
     * @param x offset from grid corner
     * @param y offset from grid corner
     * @return dot product of gradient and offset
     */
    private double grad(int hash, double x, double y) {
        int h = hash & 7; // 8 possible directions
        double u = h < 4 ? x : y;
        double v = h < 4 ? y : x;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
}
