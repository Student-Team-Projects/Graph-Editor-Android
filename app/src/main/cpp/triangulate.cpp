//
// Created by kasia on 18.01.23.
//

#include "triangulate.h"

#include <utility>
#include <vector>
#include <algorithm>
#include <stack>
#include <iostream>


std::vector<Vertex*> make_graph(int n, std::vector<std::pair<int, int>> edges_input){
    std::vector<Vertex*> V;
    for (int i = 0; i < n; ++i) {
        V.emplace_back(new Vertex(i));
    }

    for (auto i : edges_input) {
        V.at(i.first)->add_edge(V.at(i.second));
        V.at(i.second)->add_edge(V.at(i.first));
    }
    return V;
}

bool triangulate(std::vector<Vertex*>& V, std::vector<Face>& res) {
    int n = V.size();
    bool visited[n];
    int depth[n];
    int low[n];
    int parent[n];
    for(int i = 0; i<n; i++){
        visited[i] = false;
        depth[i] = -1;
        low[i] = -1;
        parent[i] = -1;
    }
    int depth_for_bi=0;
    std::vector<std::pair<int, int>> to_add;
    make_graph_connected(visited, V, to_add);
    for(auto i : to_add){
        V[i.first]->add_edge(V[i.second]);
        V[i.second]->add_edge(V[i.first]);
    }
    to_add.clear();
    for(int i = 0; i<n; i++){
        visited[i] = false;
    }
    make_graph_biconnected(0, depth_for_bi, visited, depth, low, V, parent, to_add);
    for(auto i : to_add){
        V[i.first]->add_edge(V[i.second]);
        V[i.second]->add_edge(V[i.first]);
    }
    std::vector<Face> f;
    bool is_planar = compute_faces(V, f);
    if(!is_planar){
        return false;
    }
    triangulate_biconnected_component(f, res);
    return true;
}

void make_graph_connected(bool* visited, std::vector<Vertex*> &V, std::vector<std::pair<int, int>>& toAdd) {
    mark_visited(0, V, visited);
    for( int i =0; i<V.size(); i++){
        if(!visited[i]){
            toAdd.emplace_back(0, i);
            mark_visited(i, V, visited);
        }
    }

}

void mark_visited(int i, std::vector<Vertex*> &V, bool* visited) {
    if(!visited[i]) {
        visited[i] = 1;
        for (auto v : V[i]->edges) {
            mark_visited(v.first->id, V, visited);
        }
    }
}


void make_graph_biconnected(int i, int& d, bool* visited, int* depth,
                                int* low, std::vector<Vertex*> &adj, int* parent,
                                std::vector<std::pair<int, int>>& edges_to_add){
    visited[i] = true;
    depth[i] = d+1;
    low[i] = d+1;
    d++;
    std::vector<int> children;

    for (auto ni : adj[i]->edges) {
        if(!visited[ni.first->id]){
            if(parent[i] == -1) {
                children.push_back(ni.first->id);
            }
            parent[ni.first->id] = i;
            make_graph_biconnected(ni.first->id, d, visited, depth, low, adj, parent, edges_to_add);
            low[i] = std::min(low[i], low[ni.first->id]);

            if(parent[i] == -1 && children.size() > 1){
                edges_to_add.emplace_back(children.back(), children[children.size()-2]);
            }

            if(parent[i] != -1 && low[ni.first->id] >= depth[i]) {
                edges_to_add.emplace_back(ni.first->id, parent[i]);
            }
        }
        else {
            if(ni.first->id != parent[i] && depth[ni.first->id] < low[i]){
                low[i] = depth[ni.first->id];
            }
        }
    }
}


void triangulate_biconnected_component(std::vector<Face>& faces, std::vector<Face>& triangulated_faces){
    for(auto & f : faces){
        if(f.V.size() >= 4){
            auto v1 = f.V[0]; // 0 is often used assumption do not change
            auto vj = f.vertex_have_additional_edge(v1);
            if(vj == nullptr){
                for(int i = 2; i<f.V.size()-1; i++){
                    v1->add_edge(f.V[i]);
                    f.V[i]->add_edge(v1);
                    triangulated_faces.push_back(Face({f.V[0], f.V[i], f.V[i+1]}));
                }
                triangulated_faces.push_back(Face({f.V[0], f.V[1], f.V[2]}));
            } else{
                int j = 0;
                while(f.V[j] != vj) j++;
                for(int i = 2; i<j; i++){
                    f.V[i]->add_edge(f.V[(j+1)%f.V.size()]);
                    f.V[(j+1)%f.V.size()]->add_edge(f.V[i]);
                    triangulated_faces.push_back(Face({f.V[i], f.V[(i+1)%f.V.size()], f.V[(j+1)%f.V.size()]}));
                }
                for(int i = j+1; i<f.V.size(); i++){
                    f.V[i]->add_edge(f.V[1]);
                    f.V[1]->add_edge(f.V[i]);
                    triangulated_faces.push_back(Face({f.V[1], f.V[i], f.V[(i+1)%f.V.size()]}));
                }
                triangulated_faces.push_back(Face({f.V[1], f.V[2], f.V[(j+1)%f.V.size()]}));
            }
        }
        else{
            triangulated_faces.push_back(f);
        }
    }
}

bool compute_faces(std::vector<Vertex*>& V, std::vector<Face>& faces){
    std::vector<Fragment> fragments;

    std::vector<Vertex*> first_face;
    auto start = V[0]->edges.back();
    V[0]->edges.pop_back();
    first_face = find_cycle(V[0], start.first);
    for(auto& v : V)
        v->visited= false;
    V[0]->edges.push_back(start);
    faces.emplace_back(first_face);
    faces.emplace_back(first_face);


    Vertex* last = first_face.back();
    for(auto v : first_face){
        v->mark_edge(last);
        last->mark_edge(v);
        last = v;
        v->embedded= true;
    }

    compute_fragments(V, faces, fragments);
    for(auto& v : V)
        v->visited= false;

    while(!fragments.empty()){
        for(Fragment& f : fragments){
            f.compute_faces(faces);
            for(auto& v : V)
                v->visited= false;
        }
        Fragment* fragment_to_embed = &fragments[0];
        for(Fragment& f : fragments){
            if (f.available_faces.empty()){
                return false;
            }
            if (f.available_faces.size() == 1){
                fragment_to_embed=&f;
                break;
            }
        }
        faces.push_back(fragment_to_embed->add_path());
        for(auto& v : V)
            v->visited= false;
        fragments.clear();
        compute_fragments(V, faces, fragments);
    }
    return true;
}

std::vector<Vertex*> find_cycle(Vertex* cur, Vertex* target) {
    cur->visited = true;
    for(auto v : cur->edges){
        if(v.first == target){
            return {v.first, cur};
        }
        if(v.first->visited){
            continue;
        }
        auto res = find_cycle(v.first, target);
        if(!res.empty()){
            res.push_back(cur);
            return res;
        }
    }
    return {};
}

void compute_fragments(std::vector<Vertex*> &V, std::vector<Face> faces, std::vector<Fragment> &f) {
    for(auto& face : faces){
        for(Vertex* i : face.V){
            for(auto j : i->edges) {
                if (j.first->embedded && j.first->id < i->id  && !j.second){
                    f.emplace_back(i, j.first);
                }
            }
        }
    }

    std::stack<Vertex*> q;
    Vertex* cur;
    for(auto& i : V){
        if(i->visited != 0 || i->embedded){
            continue;
        }
        std::vector<Vertex*> new_fragment;
        q.push(i);
        while(!q.empty()){
            cur = q.top();
            q.pop();
            if(cur->visited == 0) {
                new_fragment.push_back(cur);
                cur->visited = true;
                if(!cur->embedded) {
                    for (auto j: cur->edges) {
                        q.push(j.first);
                    }
                }
            }
        }
        for(auto&i : new_fragment){
            if(i->embedded){
                i->visited = false;
            }
        }
        f.emplace_back(new_fragment);
    }
}


bool Face::contain_edge(Vertex* key1, Vertex* key2){
    int f1 = std::find(V.begin(), V.end(), key1) - V.begin();
    if (f1 && V[f1 - 1] == key2) return true;
    if (f1 + 1 < V.size() && V[f1 + 1] == key2) return true;
    return false;
}

bool Face::contain(Vertex* key){
    return std::find(V.begin(), V.end(), key) != V.end();
}

bool Fragment::contain(Vertex* key){
    return std::find(V.begin(), V.end(), key) != V.end();
}

Face::Face(std::vector<Vertex*> v){
    V = std::move(v);
}

Fragment::Fragment(std::vector<Vertex*> &v) {
    V = v;
}

Fragment::Fragment(Vertex* i, Vertex* j) {
    V.push_back(i);
    V.push_back(j);
}

void Fragment::compute_faces(std::vector<Face>& all_faces) {
    std::vector<Vertex*> contact_vertices;
    for(auto& v : V){
        if(v->embedded){
            contact_vertices.push_back(v);
        }
    }

    for(auto& face : all_faces){
        bool contain_all = true;
        for(auto& key : contact_vertices){
            if(std::find(face.V.begin(), face.V.end(), key) == face.V.end()){
                contain_all= false;
                break;
            }
        }
        if(contain_all){
            available_faces.push_back(&face);
        }
    }
}

Vertex* Face::vertex_have_additional_edge(Vertex* v) {
    for(auto i : v->edges){
        i.first->visited = true;
    }
    Vertex* res = nullptr;
    for(auto i : V){
        if(i->visited){
            if(!contain_edge(v, i)){
                res = i;
                break;
            }
        }
    }
    for(auto i : v->edges){
        i.first->visited = false;
    }
    return res;
}

std::vector<Vertex *> Fragment::find_path(Vertex *vertex, Face *face){
    vertex->visited = true;
    for(auto& v : vertex->edges){
        if(v.first->visited || v.second || !(contain(v.first))){
            continue;
        }
        if(face->contain(v.first)){
            return {v.first, vertex};
        }
        if(v.first->embedded){
            continue;
        }
        auto res = find_path(v.first, face);
        if(!res.empty()){
            res.push_back(vertex);
            return res;
        }
    }
    return {};
}

Face Fragment::add_path() {
    Vertex* first_ver;
    for(auto v : V){
        if(v->embedded){
            first_ver=v;
            break;
        }
    }

    Face* face_to_split = available_faces.front();
    std::vector<Vertex*> new_path = find_path(first_ver, face_to_split);
    std::vector<Vertex*> face1 = new_path;

    Vertex* last1 = nullptr;
    for(auto v : new_path){
        if(last1 != nullptr) {
            v->mark_edge(last1);
            last1->mark_edge(v);
        }
        last1 = v;
        v->embedded= true;
    }

    for(auto v : new_path)
        v->embedded = true;

    Vertex* last = new_path.front();
    auto it = face_to_split->V.begin();
    while(*it!=new_path.back()){
        it++;
    }
    it++;
    while(it != face_to_split->V.end() && *it != last){
        face1.push_back(*it);
        it++;
    }

    if(it == face_to_split->V.end()){
        it=face_to_split->V.begin();
        while(*it != last){
            face1.push_back(*it);
            it++;
        }
    }

    std::reverse(new_path.begin(),new_path.end());
    std::vector<Vertex*> face2 = new_path;
    last = new_path.front();
    it = face_to_split->V.begin();
    while(*it!=new_path.back()){
        it++;
    }
    it++;
    while(it != face_to_split->V.end() && *it != last){
        face2.push_back(*it);
        it++;
    }

    if(it == face_to_split->V.end()){
        it=face_to_split->V.begin();
        while(*it != last){
            face2.push_back(*it);
            it++;
        }
    }
    face_to_split->V = face1;
    return Face(face2);
}

void Vertex::add_edge(Vertex *x) {
    edges.emplace_back(x, false);
}

void Vertex::mark_edge(Vertex *x) {
    for(auto & i : edges){
        if(i.first == x){
            i.second = true;
            return;
        }
    }
}

