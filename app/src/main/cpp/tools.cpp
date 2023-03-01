#include <iostream>
#include <jni.h>
#include <thread>
#include <atomic>
#include <cmath>
#include <vector>
#include <random>
#include <queue>
#include <algorithm>

#include "planarity.h"
#include "forces.h"

extern "C"
JNIEXPORT jdoubleArray JNICALL
Java_com_example_graph_1editor_jni_Tools_arrange(JNIEnv *env, jobject clazz,
                                                 jint n,
                                                 jint m,
                                                 jdoubleArray tab_x,
                                                 jdoubleArray tab_y,
                                                 jintArray tab_edge_source,
                                                 jintArray tab_edge_target) {
    ld W = 1, H = 1;
    jdoubleArray res = env->NewDoubleArray(n*2);
    jdouble res_tab[n*2];

    if (n >= 3) {
        std::vector<NamespaceForVertex::Vertex> V;
        jint *edge_source = (*env).GetIntArrayElements(tab_edge_source, nullptr);
        jint *edge_target = (*env).GetIntArrayElements(tab_edge_target, nullptr);
        jdouble *x = (*env).GetDoubleArrayElements(tab_x, nullptr);
        jdouble *y = (*env).GetDoubleArrayElements(tab_y, nullptr);
        std::vector<std::vector<int>> E(n);
        for (int i = 0; i < n; ++i) {
            V.emplace_back(i);
            V[i].setPos(x[i] / 2, y[i] / 2);
        }
        for (int i = 0; i < m; ++i) {
            E[edge_source[i]].push_back(edge_target[i]);
            E[edge_target[i]].push_back(edge_source[i]);
        }

        ld area = W * H;
        ld k = std::sqrt(area / n);
        ld t = W + H;
        ld cooling = 1.1;
        const int iterations = 200;
        for (int i = 0; i < iterations; ++i) {
            ld max_diff = 0;
            forcesIteration(V, E, k, n);
            for (auto &v : V) {
                if (v.free) {
                    auto delta = ((v.disp.module() > t) ? v.disp / v.disp.module() * t : v.disp);
                    max_diff = std::max(delta.x, max_diff);
                    max_diff = std::max(delta.y, max_diff);
                    v.pos = v.pos + delta;
                }
                v.pos.x = fmin(W, fmax(-W, v.pos.x));
                v.pos.y = fmin(H, fmax(-H, v.pos.y));
                v.disp.clear();
            }
            t = t / cooling;
            if (max_diff <= 0.001) break;
        }
        auto mnx = (*std::min_element(V.begin(), V.end(), [](auto a, auto b) {
            return a.pos.x < b.pos.x;
        })).pos.x;
        auto mny = (*std::min_element(V.begin(), V.end(), [](auto a, auto b) {
            return a.pos.y < b.pos.y;
        })).pos.y;
        auto mxx = (*std::min_element(V.begin(), V.end(), [](auto a, auto b) {
            return a.pos.x > b.pos.x;
        })).pos.x;
        auto mxy = (*std::min_element(V.begin(), V.end(), [](auto a, auto b) {
            return a.pos.y > b.pos.y;
        })).pos.y;
        for (auto &v : V) {
            v.pos.x -= mnx;
            v.pos.y -= mny;
            v.pos.x /= (mxx - mnx);
            v.pos.y /= (mxy - mny);
        }

        int j=0;
        for (auto &v : V) {
            res_tab[j] = v.pos.x;
            res_tab[j+n] = v.pos.y;
            ++j;
        }
    }
    else{
        if(n == 1) {
            res_tab[0] = 0;
            res_tab[1] = 0;
        }
        if(n == 2) {
            res_tab[0] = 0;
            res_tab[2] = 0;
            res_tab[1] = 1;
            res_tab[3] = 1;
        }
    }
    env->SetDoubleArrayRegion(res, (jsize)0, (jsize)n*2, res_tab);
    return res;
}


// SECOND FUNCTION
extern "C"
JNIEXPORT jdoubleArray JNICALL
Java_com_example_graph_1editor_jni_Tools_arrangePlanar( JNIEnv *env, jobject clazz,
                                                        jint n,
                                                        jint m,
                                                        jdoubleArray tab_x,
                                                        jdoubleArray tab_y,
                                                        jintArray tab_edge_source,
                                                        jintArray tab_edge_target) {

    std::vector<NamespaceForVertex::Vertex> V;
    jint *edge_source = (*env).GetIntArrayElements(tab_edge_source, nullptr);
    jint *edge_target = (*env).GetIntArrayElements(tab_edge_target, nullptr);
    jdouble *x = (*env).GetDoubleArrayElements(tab_x, nullptr);
    jdouble *y = (*env).GetDoubleArrayElements(tab_y, nullptr);
    std::vector<std::vector<int>> E(n);

    // normalize the vertex positions
    ld max_x = 0, max_y = 0;
    for (int i = 0; i < n; ++i) {
        if (fabs(x[i]) > max_x) max_x = fabs(x[i]);
        if (fabs(y[i]) > max_y) max_y = fabs(y[i]);
    }
    ld W = 2 * max_x, H = 2 * max_y;

    for (int i = 0; i < n; ++i) {
        V.emplace_back(i);
        V[i].setPos(x[i], y[i]);
    }
    for (int i = 0; i < m; ++i) {
        E[edge_source[i]].push_back(edge_target[i]);
        E[edge_target[i]].push_back(edge_source[i]);
    }

    ld area = W * H;
    ld k = std::sqrt(area / n);
    ld t = W + H;
    ld cooling = 1;
    const int iterations = 200;
    for (int i = 0; i < iterations; ++i) {
        // calculate repulsive forces
        forcesIteration(V, E, k, n);
        ld epsilon = smallest_vertex_edge_distance(V, E);
        ld safe_dist = 0.4 * epsilon;   // cannot be more than 0.5 * epsilon, should not be more than 0.45 * epsilon

        for (auto &v : V) {
            if (v.free) {
                ld disp_module = v.disp.module();
                if (disp_module > safe_dist) {
                    v.disp.x /= (disp_module/safe_dist);
                    v.disp.y /= (disp_module/safe_dist);
                }
                v.pos.x += v.disp.x;
                v.pos.y += v.disp.y;
            }
            v.pos.x = fmin(W / 2, fmax(-W / 2, v.pos.x));
            v.pos.y = fmin(H / 2, fmax(-H / 2, v.pos.y));
            v.disp.clear();
        }
        t = t / cooling;
    }
    jdoubleArray res = env->NewDoubleArray(n*2);
    jdouble res_tab[n*2];
    int j=0;
    double mnx = (*std::min_element(V.begin(), V.end(), [](auto a, auto b) {
        return a.pos.x < b.pos.x;
    })).pos.x;
    double mny = (*std::min_element(V.begin(), V.end(), [](auto a, auto b) {
        return a.pos.y < b.pos.y;
    })).pos.y;
    double mxx = (*std::min_element(V.begin(), V.end(), [](auto a, auto b) {
        return a.pos.x > b.pos.x;
    })).pos.x;
    double mxy = (*std::min_element(V.begin(), V.end(), [](auto a, auto b) {
        return a.pos.y > b.pos.y;
    })).pos.y;
    for (NamespaceForVertex::Vertex &v : V) {
        v.pos.x -= mnx;
        v.pos.y -= mny;
        v.pos.x /= (mxx - mnx);
        v.pos.y /= (mxy - mny);
    }

    for (NamespaceForVertex::Vertex &v : V) {
        res_tab[j] = v.pos.x;
        res_tab[j+n] = v.pos.y;
        ++j;
    }
    env->SetDoubleArrayRegion(res, (jsize)0, (jsize)n*2, res_tab);
    return res;
}


// THIRD FUNCTION

extern "C"
JNIEXPORT jdoubleArray JNICALL
Java_com_example_graph_1editor_jni_Tools_makePlanar( JNIEnv *env, jobject clazz,
                                                     jint n,
                                                     jint m,
                                                     jdoubleArray tab_x,
                                                     jdoubleArray tab_y,
                                                     jintArray tab_edge_source,
                                                     jintArray tab_edge_target) {
    jint *edge_source = (*env).GetIntArrayElements(tab_edge_source, nullptr);
    jint *edge_target = (*env).GetIntArrayElements(tab_edge_target, nullptr);
    jdouble *x = (*env).GetDoubleArrayElements(tab_x, nullptr);
    jdouble *y = (*env).GetDoubleArrayElements(tab_y, nullptr);

    jdoubleArray res = env->NewDoubleArray(n*2);
    jdouble res_tab[n*2];

    if(n >= 3) {
        std::vector<std::vector<int>> E(n);
        std::vector<std::pair<int, int>> edges_input;
        edges_input.reserve(m);
        for (int i = 0; i < m; ++i) {
            edges_input.emplace_back(edge_target[i], edge_source[i]);
        }

        auto G = make_graph(n, edges_input);
        std::vector<Face> faces;
        bool is_planar = triangulate(G, faces);
        if (!is_planar) {
            res_tab[0] = -6969696969696969.;
            env->SetDoubleArrayRegion(res, (jsize)0, (jsize)n*2, res_tab);
            return res;
        }
        auto orig_order_set = compressFaces(faces);
        auto orig_order = std::vector<Vertex*>(orig_order_set.begin(), orig_order_set.end());
        auto res1 = findCanonicalOrder(faces);
        auto res2 = shiftMethod(res1);
        auto res3 = extractCoordinates(orig_order, res2);

        int j=0;
        double mnx = (*std::min_element(res3.begin(), res3.end(), [](auto a, auto b) {
            return a.second.x < b.second.x;
        })).second.x;
        double mny = (*std::min_element(res3.begin(), res3.end(), [](auto a, auto b) {
            return a.second.y < b.second.y;
        })).second.y;
        double mxx = (*std::max_element(res3.begin(), res3.end(), [](auto a, auto b) {
            return a.second.x < b.second.x;
        })).second.x;
        double mxy = (*std::max_element(res3.begin(), res3.end(), [](auto a, auto b) {
            return a.second.y < b.second.y;
        })).second.y;

        for (auto &v : res3) {
            v.second.x -= mnx;
            v.second.y -= mny;
            v.second.x /= (mxx - mnx);
            v.second.y /= (mxy - mny);
        }

        for (auto &v : res3) {
            res_tab[j] = v.second.x;
            res_tab[j+n] = v.second.y;
            ++j;
        }
    }
    else{
        if(n == 1) {
            res_tab[0] = 0;
            res_tab[1] = 0;
        }
        if(n == 2) {
            res_tab[0] = 0;
            res_tab[2] = 0;
            res_tab[1] = 1;
            res_tab[3] = 1;
        }
    }

    env->SetDoubleArrayRegion(res, (jsize)0, (jsize)n*2, res_tab);
    return res;
}
