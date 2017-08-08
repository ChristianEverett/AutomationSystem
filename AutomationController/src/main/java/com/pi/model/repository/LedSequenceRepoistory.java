package com.pi.model.repository;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Repository;

import com.pi.model.LedSequence;

@Repository(RepositoryType.LedSequence)
public class LedSequenceRepoistory extends BaseRepository<String, LedSequence>
{
	private Map<String, LedSequence> sequences = new HashMap<>();
}
