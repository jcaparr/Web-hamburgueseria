package com.hamburguesas.service;

import com.hamburguesas.dto.HamburgueseriaDto;
import com.hamburguesas.exception.ConflictException;
import com.hamburguesas.exception.ResourceNotFoundException;
import com.hamburguesas.model.Hamburgueseria;
import com.hamburguesas.model.ListaDeseados;
import com.hamburguesas.model.Usuario;
import com.hamburguesas.repository.CalificacionRepository;
import com.hamburguesas.repository.HamburgueseriaRepository;
import com.hamburguesas.repository.ListaDeseadosRepository;
import com.hamburguesas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final ListaDeseadosRepository listaDeseadosRepository;
    private final HamburgueseriaRepository hamburgueseriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CalificacionRepository calificacionRepository;

    public List<HamburgueseriaDto> listar(Long usuarioId) {
        return listaDeseadosRepository.findByUsuario_IdOrderByFechaDesc(usuarioId).stream()
            .map(ld -> toDto(ld.getHamburgueseria()))
            .toList();
    }

    @Transactional
    public void agregar(Long usuarioId, Long hamburgueseriaId) {
        if (listaDeseadosRepository.existsByUsuario_IdAndHamburgueseria_Id(usuarioId, hamburgueseriaId)) {
            throw new ConflictException("Ya esta en tu lista de deseados");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Hamburgueseria hamburgueseria = hamburgueseriaRepository.findById(hamburgueseriaId)
            .orElseThrow(() -> new ResourceNotFoundException("Hamburgueseria no encontrada"));

        listaDeseadosRepository.save(
            ListaDeseados.builder().usuario(usuario).hamburgueseria(hamburgueseria).build()
        );
    }

    @Transactional
    public void quitar(Long usuarioId, Long hamburgueseriaId) {
        ListaDeseados item = listaDeseadosRepository
            .findByUsuario_IdAndHamburgueseria_Id(usuarioId, hamburgueseriaId)
            .orElseThrow(() -> new ResourceNotFoundException("No estaba en tu lista de deseados"));
        listaDeseadosRepository.delete(item);
    }

    private HamburgueseriaDto toDto(Hamburgueseria h) {
        Double promedio = calificacionRepository.promedioPorHamburgueseria(h.getId());
        long cantidad = calificacionRepository.countByHamburgueseria_Id(h.getId());
        return new HamburgueseriaDto(
            h.getId(), h.getNombre(), h.getDireccion(), h.getZona(), h.getFotoUrl(),
            h.getLatitud(), h.getLongitud(), promedio, cantidad, true
        );
    }
}
